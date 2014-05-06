package fielded;

import field.utility.Triple;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Created by marc on 3/28/14.
 */
public class Animatable {


	static public interface AnimationElement {

		default public Object beginning(boolean isEnding) { return this; }
		public Object middle(boolean isEnding);
		default public Object end(boolean isEnding) { return this; }
	}

	static public class Shim implements Supplier<Boolean>, Consumer<Boolean>
	{
		private final AnimationElement e;
		boolean stopping = false;
		boolean first = true;

		public Shim(AnimationElement e)
		{
			this.e = e;
		}

		@Override
		public Boolean get() {
			if (first)
			{
				first = false;
				e.beginning(stopping);
				return true;
			}
			else if (stopping)
			{
				e.end(stopping);
				return false;
			}
			else
			{
				e.middle(stopping);
				return true;
			}
		}

		@Override
		public void accept(Boolean willContinue) {
			stopping = !willContinue;
		}
	}

	static List<BiFunction<AnimationElement, Object, AnimationElement>> handlers = new ArrayList<>();

	static public void registerHandler(BiFunction<AnimationElement, Object, AnimationElement> h)
	{
		handlers.add(0, h);
	}

	static public AnimationElement interpret(Object r, AnimationElement current) {
		for (BiFunction<AnimationElement, Object, AnimationElement> b : handlers) {
			current = b.apply(current, r);
		}
		return current;
	}

}
