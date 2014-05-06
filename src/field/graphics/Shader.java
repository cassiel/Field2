package field.graphics;

import field.utility.Dict;
import org.lwjgl.opengl.*;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.lwjgl.opengl.GL20.*;

public class Shader extends BaseScene<Shader.State> implements Scene.Perform {

	static public final Dict.Prop<Shader> currentShader = new Dict.Prop<>("currentShader");

	static public class State extends BaseScene.Modifiable {
		int name;
		boolean work = true;
		boolean valid = false;
	}

	public enum Type {
		vertex(GL20.GL_VERTEX_SHADER), geometry(GL32.GL_GEOMETRY_SHADER), fragment(GL20.GL_FRAGMENT_SHADER), tessControl(GL40.GL_TESS_CONTROL_SHADER), tessEval(GL40.GL_TESS_EVALUATION_SHADER), compute(GL43.GL_COMPUTE_SHADER);

		public int gl;

		private Type(int gl) {
			this.gl = gl;
		}
	}

	public interface iErrorHandler {
		public void beginError();

		public void errorOnLine(int line, String error);

		public void endError();

		public void noError();
	}

	protected iErrorHandler onError = null;

	Map<Type, Source> source = new LinkedHashMap<>();

	static public class Source {
		public String source;
		protected final Type type;
		protected int status;
		public iErrorHandler onError = null;

		protected Set<Integer> attachedTo = new LinkedHashSet<>();

		public Source(String source, Type type) {
			this.type = type;
			this.source = source;
			this.status = 0;
		}

		public class State {
			String source;
			int name = -1;
			boolean good = false;
		}

		protected boolean clean() {
			State s = GraphicsContext.get(this, () -> {
				State state = new State();
				state.source = source;
				return state;
			});

			if (!s.source.equals(this.source)) {
				for (Integer ii : attachedTo)
					GL20.glDetachShader(ii, s.name);
				attachedTo.clear();
				GL20.glDeleteShader(s.name);

				s = new State();
				s.source = source;
				GraphicsContext.put(this, s);
			}

			if (GraphicsContext.trace) System.out.println(" shader name " + s.name);

			if (s.name == -1) {
				System.out.println(" compiling ");
				s.name = GL20.glCreateShader(type.gl);
				GL20.glShaderSource(s.name, s.source);
				GL20.glCompileShader(s.name);
				status = GL20.glGetShaderi(s.name, GL20.GL_COMPILE_STATUS);

				System.out.println(" status " + status);
				if (status == 0) {
					String ret = GL20.glGetShaderInfoLog(s.name, 10000);
					System.err.println(type + " program failed to compile");
					System.err.println(" log is <" + ret + ">");
					System.err.println(" shader source is <" + source + ">");
					if (onError != null) {
						onError.beginError();
						String log = ret;
						String[] lines = log.split("\n");
						for (String ll : lines) {
							try {
								String[] ss = ll.split(":");
								if (ss.length > 2) {
									int ii = Integer.parseInt(ss[2]);
									onError.errorOnLine(ii, ll);
								}
							} catch (NumberFormatException e) {
								try {
									Matcher q = Pattern.compile(".*?\\((.*?)\\)").matcher(ll);
									q.find();
									String g = q.group(1);
									int ii = Integer.parseInt(g);
									onError.errorOnLine(ii, ll);
								} catch (Exception e2) {
									e2.printStackTrace();
								}
							}
						}
						onError.endError();
					}
					System.out.println(" shader is not good");
					s.good = false;
					return false;
				} else {
					if (onError != null) onError.noError();

					System.out.println(" shader is good ");

					s.good = true;
					return true;
				}
			}
			return false;
		}

		public void finalize()
		{
			GraphicsContext.postQueueInAllContexts(this::destroy);
		}

		protected void destroy() {
			State s = GraphicsContext.remove(this);
			if (s != null) GL20.glDeleteShader(s.name);
		}
	}

	protected State setup() {
		State s = new State();
		s.name = GL20.glCreateProgram();
		return s;
	}

	public Source addSource(Type type, String source)
	{
		Source s = new Source(source, type);
		this.source.put(type, s);
		return s;
	}


	@Override
	protected void deallocate(State s) {
		GL20.glDeleteProgram(s.name);
		// doesn't kill the shaders, because, technically they could be attached elsewhere
	}

	protected boolean perform0() {
		boolean work = false;

		if (GraphicsContext.trace) System.out.println(" checking :" + source.keySet());

		for (Map.Entry<Type, Source> s : source.entrySet()) {
			work |= s.getValue().clean();
		}

		State name = GraphicsContext.get(this);

		work |= name.work;
		name.work = false;

		if (work) {
			name.valid = true;
			for (Map.Entry<Type, Source> s : source.entrySet()) {
				Source.State state = GraphicsContext.get(s.getValue());
				if (s.getValue().status != 0 && state.name != -1 && !s.getValue().attachedTo.contains(name.name)) {
					System.out.println(" attaching :" + s.getKey() + " / " + s.getValue() + " " + name.name);
					GL20.glAttachShader(name.name, state.name);
					s.getValue().attachedTo.add(name.name);
				}
			}

			System.out.println(" --- binding frag data --- ");
			GL30.glBindFragDataLocation(name.name, 0, "_output");
			GL30.glBindFragDataLocation(name.name, 0, "_output0");
			GL30.glBindFragDataLocation(name.name, 1, "_output1");
			GL30.glBindFragDataLocation(name.name, 2, "_output2");
			GL30.glBindFragDataLocation(name.name, 3, "_output3");

			for (int i = 1; i < 16; i++)
				glBindAttribLocation(name.name, i, "attribute" + i);

			System.out.println(" linking .... ");
			glLinkProgram(name.name);
			int linkStatus = glGetProgrami(name.name, GL20.GL_LINK_STATUS);
			if (linkStatus == 0) {
				String ret = GL20.glGetProgramInfoLog(name.name, 10000);
				System.err.println(" program failed to link");
				System.err.println(" log is <" + ret + ">");
				if (onError != null) {
					onError.beginError();
					onError.errorOnLine(-1, ret);
					onError.endError();
				}
				name.valid = false;
			}
			System.out.println(" validating .... ");
			glValidateProgram(name.name);
			int validateStatus = glGetProgrami(name.name, GL20.GL_VALIDATE_STATUS);
			if (validateStatus == 0) {
				String ret = GL20.glGetProgramInfoLog(name.name, 10000);
				System.err.println(" program failed to validate");
				System.err.println(" log is <" + ret + ">");
				if (onError != null) {
					onError.beginError();
					onError.errorOnLine(-1, ret);
					onError.endError();
				}
				name.valid = false;
			}
		}

		if (name.valid) {
			if (GraphicsContext.trace) System.out.println(" using program " + name.name);
			GraphicsContext.put(currentShader, this);
			GL20.glUseProgram(name.name);
		}

		return true;
	}

	@Override
	protected boolean perform1() {
		GraphicsContext.put(currentShader, null);
		GL20.glUseProgram(0);
		return true;
	}

	public Integer getOpenGLName() {
		State s = GraphicsContext.get(this, () -> null);
		if (s == null) return null;
		return s.name;
	}

	@Override
	public int[] getPasses() {
		return new int[]{-2, 2};
	}
}
