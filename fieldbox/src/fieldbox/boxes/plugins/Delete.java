package fieldbox.boxes.plugins;

import com.badlogic.jglfw.Glfw;
import fieldbox.boxes.*;

import java.util.stream.Stream;

/**
 * Adds: Press command/meta delete to delete selected boxes, swipe down to delete selection
 */
public class Delete extends Box {

	protected final Box root;

	public Delete(Box root) {
		this.root = root;
		this.properties.putToList(Keyboard.onKeyDown, (event, key) -> {
			if (event.after.isSuperDown() && key == Glfw.GLFW_KEY_DELETE) {
				Stream<Box> all = selected();
				all.forEach(bb -> bb.disconnectFromAll());
				Drawing.dirty(Delete.this);
			}
			return null;
		});


		properties.put(MarkingMenus.menu, (event) -> {
			if (selected().findAny().isPresent()) {
				MarkingMenus.MenuSpecification spec = new MarkingMenus.MenuSpecification();
				long count = selected().count();
				spec.items.put(MarkingMenus.Position.S, new MarkingMenus.MenuItem("Delete "+count+" box"+(count==1 ? "" : "es"), () -> {
					Stream<Box> all = selected();
					all.forEach(bb -> bb.disconnectFromAll());
					Drawing.dirty(Delete.this);
				}));
				return spec;
			}
			return null;
		});

	}

	private Stream<Box> selected() {
		return root.breadthFirst(root.downwards()).filter(x -> x.properties.isTrue(Mouse.isSelected, false));
	}

}
