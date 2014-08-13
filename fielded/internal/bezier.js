function addSlider() {
	console.log("test1");
	var selection_start = cm.getCursor(true);
	var selection_end   = cm.getCursor(false);

	console.log("test2");

	var widget_div = $('<div class="rui-slider" data-slider="{min:0,max:100,update:\'my-input\'}"><div class="level"></div><div class="handle"></div></div>')[0]
	console.log("test3");
	var widget  = cm.addWidget(selection_start, widget_div);
	console.log("test4");
}