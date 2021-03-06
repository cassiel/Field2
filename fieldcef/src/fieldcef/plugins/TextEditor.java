package fieldcef.plugins;

import com.badlogic.jglfw.Glfw;
import field.app.RunLoop;
import field.graphics.Window;
import field.linalg.Vec2;
import field.utility.Dict;
import field.utility.Log;
import field.utility.Rect;
import fieldagent.Main;
import fieldbox.boxes.*;
import fieldbox.io.IO;
import fieldbox.ui.FieldBoxWindow;
import fieldcef.browser.Browser;
import fielded.Commands;
import fielded.ServerSupport;
import fieldnashorn.annotations.HiddenInAutocomplete;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * This is a browser, created by default, that embeds the text editor. We're checking for latency here
 */
public class TextEditor extends Box implements IO.Loaded {

	static public final Dict.Prop<TextEditor> textEditor = new Dict.Prop<>("textEditor").toCannon()
											    .type()
											    .doc("The TextEditor that is stuck in front of the window, in window coordinates");
	private final Box root;
	@HiddenInAutocomplete
	public Browser browser;
	@HiddenInAutocomplete
	public String styles;
	List<String> playlist = Arrays.asList("preamble.js", "jquery-2.1.0.min.js", "jquery.autosize.input.js", "modal.js");
	String styleSheet = "field-codemirror.css";
	// we'll need to make sure that this is centered on larger screens
	int maxw = 800;
	int maxh = 900;
	int tick = 0;
	Commands commandHelper = new Commands();
	long lastTriggerAt = -1;
	int ignoreHide = 0;
	private int maxhOnCreation = 0 ;

	public TextEditor(Box root) {
		this.properties.put(textEditor, this);
		this.root = root;
	}

	@HiddenInAutocomplete
	private static String readFile(String s, boolean append) {
		try (BufferedReader r = new BufferedReader(new FileReader(new File(s)))) {
			String line = "";
			while (r.ready()) {
				line += r.readLine() + "\n";
			}

			if (append) line += "\n//# sourceURL=" + s;
			return line;

		} catch (IOException e) {
			e.printStackTrace();
		}
		return "";
	}

	@HiddenInAutocomplete
	public void loaded() {
		Log.log("texteditor.debug", "initializing browser");

		FieldBoxWindow window = this.find(Boxes.window, this.both())
					    .findFirst()
					    .get();

		Drawing drawing = root.first(Drawing.drawing)
				      .orElseThrow(() -> new IllegalArgumentException(" can't install text-drawing into something without drawing support"));


		maxh = window.getHeight() - 25 - 10 - 10 - 2;


		browser = new Browser();

		Vec2 v = drawing.windowSystemToDrawingSystem(new Vec2(window.getWidth() - maxw - 10, 10));
		Vec2 vd = drawing.windowSystemToDrawingSystemDelta(new Vec2(maxw, 1500));

		browser.properties.put(Box.frame, new Rect(v.x, v.y, vd.x, vd.y));

		maxhOnCreation = 1500;

		browser.pauseForBoot();

		this.properties.put(FLineDrawing.layer, "glass");

		browser.properties.put(FLineDrawing.layer, "glass");
		browser.properties.put(Drawing.windowSpace, new Vec2(1, 0));
		browser.properties.put(Boxes.dontSave, true);
		browser.properties.put(Box.hidden, true);
		browser.properties.put(Mouse.isSticky, true);

		browser.properties.put(FrameManipulation.lockHeight, true);
		browser.properties.put(FrameManipulation.lockWidth, true);
		browser.properties.put(FrameManipulation.lockX, true);
		browser.properties.put(FrameManipulation.lockY, true);

		browser.properties.put(Box.undeletable, true);

		browser.properties.put(Box.name, "texteditor");

		browser.connect(root);
		browser.loaded();


		executeJavaScript("$(\".CodeMirror\").css(\"height\", "+(maxh-10)+")");
		executeJavaScript("$(\".CodeMirror\").css(\"width\", "+maxw+")");


		this.properties.put(Boxes.dontSave, true);
		styles = findAndLoad(styleSheet, false);

		long[] t = {0};
		RunLoop.main.getLoop()
			    .attach(x -> {
				    if (t[0] == 0) t[0] = System.currentTimeMillis();
				    if (System.currentTimeMillis() - t[0] > 1000) {
					    boot();

					    return false;
				    }

				    return true;
			    });


		find(Watches.watches, both()).forEach(w -> {

			w.getQueue()
			 .register(x -> x.equals("selection.changed"), c -> {
				 Log.log("shy", "selection is now" + selection().count());

				 if (selection().count() != 1) {
					 browser.properties.put(Box.hidden, true);
					 Drawing.dirty(this);
				 } else {
					 browser.properties.put(Box.hidden, false);
					 Drawing.dirty(this);
				 }

			 });

		});

		first(Boxes.window, both()).ifPresent(x -> x.addKeyboardHandler(event -> {
			Set<Integer> kpressed = Window.KeyboardState.keysPressed(event.before, event.after);
			if (kpressed.contains(Glfw.GLFW_KEY_LEFT_SHIFT) || kpressed.contains(Glfw.GLFW_KEY_RIGHT_SHIFT)) {
				if (event.after.keysDown.size() == 1) trigger();
			}

			return true;
		}));

		RunLoop.main.getLoop()
			    .attach(x -> {

				    int maxh = window.getHeight() - 25 - 10 - 10 - 2;
				    Rect f = browser.properties.get(Box.frame);
				    if (f.h!=Math.min(maxhOnCreation-40, maxh))
				    {
					    f = f.duplicate();
					    executeJavaScript("$(\".CodeMirror\").css(\"height\", "+Math.min(maxh, maxhOnCreation-40)+")");
				    }

				    return true;
			    });

	}

	@HiddenInAutocomplete
	public void trigger() {
		long now = System.currentTimeMillis();
		if (now - lastTriggerAt < 500) {
			if (!browser.getFocus()) browser.executeJavaScript_queued("_messageBus.publish('focus', {})");
			else browser.executeJavaScript_queued("_messageBus.publish('de" + "focus', {})");
			browser.setFocus(!browser.getFocus());
		}
		lastTriggerAt = now;
	}

	@HiddenInAutocomplete
	public void boot() {
		browser.properties.put(browser.url, "http://localhost:"+ ServerSupport.webserverPort+"/init");
		Drawing.dirty(this);
		browser.finishBooting();
	}

	@HiddenInAutocomplete
	public void show() {
		browser.properties.put(Box.hidden, false);
		browser.setFocus(true);
		Drawing.dirty(browser);
	}

	@HiddenInAutocomplete
	public void hide() {
		tick = 0;
		RunLoop.main.getLoop()
			    .attach(x -> {
				    if (tick == 5) {
					    browser.properties.put(Box.hidden, true);
					    Drawing.dirty(this);
				    }
				    tick++;
				    return tick != 5;
			    });
		browser.setFocus(false);
		Drawing.dirty(browser);
	}

	@HiddenInAutocomplete
	public void runCommands() {
		browser.executeJavaScript("goCommands()");
		show();
	}

	@HiddenInAutocomplete
	public void center() {
		FieldBoxWindow window = this.find(Boxes.window, both())
					    .findFirst()
					    .get();
		Rect f = browser.properties.get(Box.frame);
		f.x = (int) ((window.getWidth() - f.w) / 2);
		f.y = (int) ((window.getHeight() - f.h) / 2);
		if (!browser.properties.isTrue(Box.hidden, false)) Drawing.dirty(this);
	}

	@HiddenInAutocomplete
	private String findAndLoad(String f, boolean append) {

		String[] roots = {Main.app + "/fielded/internal/", Main.app + "/fielded/external/", Main.app + "/fieldcef/internal"};
		for (String s : roots) {
			if (new File(s + "/" + f).exists()) return readFile(s + "/" + f, append);
		}
		Log.log("glassbrowser.error", "Couldnt' find file in playlist :" + f);
		return null;
	}

	@HiddenInAutocomplete
	private Stream<Box> selection() {
		return breadthFirst(both()).filter(x -> x.properties.isTrue(Mouse.isSelected, false));
	}


	/**
	 * Injects css into the text editor. For example '_.textEditor.injectCSS("body {font-size:20px;}"' will give you a markedly bigger font.
	 */
	public void injectCSS(String css) {
		browser.injectCSS(css);
	}

	/**
	 * Executes some javascript directly in the text editor. For larger amounts of TextEditor coding, mark a box as "Bridge to Editor" with the command menu.
	 */
	public void executeJavaScript(String js) {
		browser.executeJavaScript_queued(js);
	}

	/**
	 * reloads this text editor. Useful if you are hacking on the CSS or JavaScript that backs the editor
	 */
	public void reload()
	{
		browser.reload();
	}


	public void setURL(String url)
	{
		browser.properties.put(browser.url, url);
	}
}

