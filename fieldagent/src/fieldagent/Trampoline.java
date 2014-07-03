package fieldagent;

import com.google.common.io.ByteStreams;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandlerFactory;
import java.util.Arrays;
import java.util.LinkedHashSet;

/**
 * Created by marc on 7/1/14.
 */
public class Trampoline {

	static public final boolean traceLoader = false;

	static protected Transform transform = new Transform();


	static public class ExtensibleClassloader extends URLClassLoader {

		public ExtensibleClassloader(URL[] urls, ClassLoader parent) {
			super(urls, parent);
		}

		public ExtensibleClassloader(URL[] urls) {
			super(urls);
		}

		public ExtensibleClassloader(URL[] urls, ClassLoader parent, URLStreamHandlerFactory factory) {
			super(urls, parent, factory);
		}

		public void addURL(URL url) {
			super.addURL(url);
		}


		public Class loadClass(String name) throws ClassNotFoundException {
			if (!shouldLoad(name)) return super.loadClass(name);
			return loadClass(name, false);
		}

		protected Class loadClass(String name, boolean resolve) throws ClassNotFoundException {
			if (!shouldLoad(name)) return super.loadClass(name, resolve);

			if (traceLoader) System.out.println("C(lc): " + name);

			// First, check if the class has already been loaded
			Class c = findLoadedClass(name);

			if (traceLoader) System.out.println("C(lc): already loaded ? " + c);

			// if not loaded, search the local (child) resources
			if (c == null) {
				try {
					c = findClass(name);
					if (traceLoader) System.out.println("C(lc): found  " + c);
				} catch (ClassNotFoundException cnfe) {
					{
						try (InputStream where = getResourceAsStream(name.replace(".", "/") + ".class")) {
							if (where != null) {
								byte[] b = ByteStreams.toByteArray(where);
								b = transformClass(name, b);
								c = defineClass(name, b, 0, b.length);
								if (traceLoader) System.out.println(" loaded ");
							}
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			}

			// if we could not find it, delegate to parent
			// Note that we don't attempt to catch any ClassNotFoundException
			if (c == null) {
				if (getParent() != null) {
					c = getParent().loadClass(name);
				} else {
					c = getSystemClassLoader().loadClass(name);
				}
			}

			if (resolve) {
				resolveClass(c);
			}

			return c;
		}

		protected byte[] transformClass(String name, byte[] b) {
			return transform.transform(name, b);
		}


		LinkedHashSet<String> blacklist_prefix = new LinkedHashSet<String>(Arrays.asList("java", "sun", "jdk", "javax", "sunw", "apple", "com.apple"));

		protected boolean shouldLoad(String name) {

			if (!name.contains(".")) return true;

			String[] s = name.split("\\.");

			for (int i = 1; i < s.length; i++) {
				String m = "";
				for (int q = 0; q < i; q++) {
					m += (q == 0 ? "" : ".") + s[q];
				}

				if (blacklist_prefix.contains(m)) return false;
			}


			return true;
		}

		public URL getResource(String name) {
			URL url = findResource(name);

			// if local search failed, delegate to parent
			if (url == null) {
				url = getParent().getResource(name);
			}

			if (traceLoader) System.out.println("C: " + name + " -> " + url);
			if (traceLoader) if (url==null) System.out.println(" URL search paths are "+Arrays.asList(getURLs()));

			return url;
		}
	}

	static public void main(String[] a) {

		if (a.length == 0) {
			System.err.println(" No main.class specified. Add one to the command line");
			System.exit(1);
		}

		String mainClass = a[0];
		String[] a2 = new String[a.length - 1];
		System.arraycopy(a, 1, a2, 0, a.length - 1);

		ExtensibleClassloader classloader = new ExtensibleClassloader(new URL[]{}, Thread.currentThread().getContextClassLoader());

		Thread.currentThread().setContextClassLoader(classloader);

		try {
			Class clazz = classloader.loadClass(mainClass);
			Method m = clazz.getMethod("main", String[].class);
			System.err.println(" -- m -- " + m);
			m.invoke(null, new Object[]{a2});
		} catch (Throwable t) {
			System.err.println(" Exception thrown in main of " + mainClass);
			t.printStackTrace();
			System.exit(1);
		}
	}

	static public void addURL(URL n) {
		ClassLoader c = Thread.currentThread().getContextClassLoader();
		try {
			c.getClass().getMethod("addURL", URL.class).invoke(c, n);
		} catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
			e.printStackTrace();
		}
	}
}