/* global picmoPopup */

const emojiData = await (await fetch(CONTEXT_PATH + "/scripts/emojibase/data.json")).json();
const messages = await (await fetch(CONTEXT_PATH + "/scripts/emojibase/messages.json")).json();

$(document).on("click", ".emoji-button", function () {
	var cont = $(this).closest(".emoji-picker-container");
	if (cont.length) {
		if (!cont.data("emoji-picker")) {
			//var rootElem = $(this).closest("form").get(0) || cont.get(0);
			var picker = picmoPopup.createPopup({
				emojiData: emojiData,
				messages: messages,
				rootElement: cont.get(0),
				showCloseButton: false,
				showPreview: false,
				showSearch: true,
				hideOnClickOutside: true,
				showCategoryTabs: false,
				hideOnEmojiSelect: false,
				autoFocusSearch: true,
				emojisPerRow: 10,
				visibleRows: 7
			}, {
				className: "",
				position: "auto-end",
				triggerElement: this,
				referenceElement: this
			});
			picker.addEventListener("emoji:select", function (e) {
				cont.trigger("emoji:select", e);
			});
			cont.data("emoji-picker", picker);
		}
		cont.data("emoji-picker").toggle();
	}
});
