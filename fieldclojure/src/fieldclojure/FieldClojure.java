package fieldclojure;


import field.graphics.FLine;
import field.graphics.RunLoop;
import field.linalg.Vec4;
import field.utility.Cached;
import field.utility.Log;
import field.utility.Pair;
import field.utility.Rect;
import fieldbox.boxes.Box;
import fieldbox.boxes.Drawing;
import fieldbox.boxes.Mouse;
import fielded.RemoteEditor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static fieldbox.boxes.FLineDrawing.frameDrawing;
import static fieldbox.boxes.StandardFLineDrawing.*;

/**
 * Created by marc on 7/15/14.
 */
public class FieldClojure extends Box {

	private final ClojureExecution clojureExecution;

	public FieldClojure(Box root) {
		Log.log("startup.clojure", "Clojure plugin is starting up ");

		clojureExecution = new ClojureExecution();

		root.connect(clojureExecution);

		properties.put(RemoteEditor.commands, () -> {

			Map<Pair<String, String>, Runnable> m = new LinkedHashMap<>();
			List<Box> selected = selection().collect(Collectors.toList());
			if (selected.size() == 1) {
				if (selected.get(0).properties.isTrue(ClojureExecution.bridgedToClojure, false)) {
					m.put(new Pair<>("Remove bridge to Clojure", "No longer will this box execute be written in Clojure"), () -> {
						disconnectFromProcessing(selected.get(0));
					});
				} else {
					m.put(new Pair<>("Bridge to Clojure", "This box will be written in Clojure"), () -> {
						connectToProcessing(selected.get(0));
					});
				}
			}
			return m;
		});


		Log.log("startup.clojure", " searching for boxes that need clojure support ");

		// we delay this for one update cycle to make sure that everybody has loaded everything that they are going to load
		RunLoop.main.once(() -> {
			root.breadthFirst(both()).forEach(box -> {
				if (box.properties.isTrue(ClojureExecution.bridgedToClojure, false)) {
					connectToProcessing(box);
				}
			});
		});

		Log.log("startup.clojure", "Clojure plugin has finished starting up ");

	}


	protected void connectToProcessing(Box box) {
		clojureExecution.connect(box);
		box.properties.put(ClojureExecution.bridgedToClojure, true);

		box.properties.putToMap(frameDrawing, "_clojureBadge_", new Cached<Box, Object, FLine>((b, was) -> {

			Rect rect = box.properties.get(Box.frame);
			if (rect == null) return null;

			FLine f = new FLine();
			f.attributes.put(hasText, true);
			f.attributes.put(fillColor, new Vec4(0, 0, 0.25f, 0.5f));
			f.moveTo(rect.x + rect.w - 7, rect.y + rect.h - 5);
			f.nodes.get(f.nodes.size() - 1).attributes.put(text, "C");

			return f;

		}, (b) -> new Pair(b.properties.get(ClojureExecution.bridgedToClojure), b.properties.get(Box.frame))));
		Drawing.dirty(box);

	}

	protected void disconnectFromProcessing(Box box) {
		clojureExecution.disconnect(box);
		box.properties.remove(ClojureExecution.bridgedToClojure);
		box.properties.removeFromMap(frameDrawing, "_clojureBadge_");
	}

	private Stream<Box> selection() {
		return breadthFirst(both()).filter(x -> x.properties.isTrue(Mouse.isSelected, false));
	}

}