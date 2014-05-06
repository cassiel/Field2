package field.utility;

import field.graphics.Bracketable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by marc on 3/19/14.
 */
public class Util {
	static public boolean safeEq(Object a, Object b) {
		if (a == null) return b == null;
		if (b == null) return false;
		return a.equals(b);
	}

	/**
	 * exception munging list autoclosable
	 */
	static public AutoCloseable closeable(Collection<? extends AutoCloseable> c) {

		for(AutoCloseable cc : c)
		{
			if (cc instanceof Bracketable) ((Bracketable)cc).open();
		}

		return () -> {
			List<Throwable> thrown = new ArrayList<>();
			c.forEach((autoCloseable) -> {
				try {
					autoCloseable.close();
				} catch (Exception e) {
					e.printStackTrace();
					thrown.add(e);
				}
			});
			if (thrown.size()>0)
			{
				Exception e = new Exception(" exception(s) throw during close "+thrown);
				e.initCause(thrown.get(0));
				throw e;
			}
		};
	}
	/**
	 * exception munging list autoclosable
	 */

	static public AutoCloseable closeable(AutoCloseable... c1) {
		return closeable(Arrays.asList(c1));
	}



}
