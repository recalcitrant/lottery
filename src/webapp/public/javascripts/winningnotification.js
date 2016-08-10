$(function () {

	objDialog.init();

	$(document).on("click", ".rm_notification_file_upload", function () {
		// qtip does not work as we'd have to bind to a future-event here:
		//$('.rm_prize_file_upload').qtip({content:{text:"löschen"}});
		var id = $(this).attr("id");
		var nid = id.substring("rm_notification_file_upload_".length);
		$.post("/winningnotification/rmupload/" + nid, function (json) {
			if ("ok" == json.status) $("#" + id).closest("tr").remove();
			else $("#message").removeClass("hidden").html(json.msg);
		}, "json");
	});

	$('.upload_file').qtip({content: {text: "Fileupload"}});
	$('.rm_drawing_base').qtip({content: {text: "Löschen"}});

	// description
	var edit_winning_notification_form = $("#winning_notification_form");
	edit_winning_notification_form.on("click", ".save_winning_notification_content", function () {
		var attrid = $(this).attr("id");
		var did = attrid.substring("save_winning_notification_content_".length, attrid.indexOf("-"));
		var hid = attrid.substring(attrid.indexOf("-") + 1);
		var description = $.trim($("#winning_notification_description").val());
		if (1024 >= description.length) {
			var url = "/winningnotification/description/update/" + did + "/" + hid;
			$.ajax({
				url: url,
				type: "POST",
				data: JSON.stringify(description),
				contentType: "application/json; charset=utf-8",
				dataType: "json", success: function (json) {
					$("#message").removeClass("hidden").html(json.msg);
				}});
		} else {
			$("#message").removeClass("hidden").html("Bitte beschränken Sie die Beschreibung auf 1024 Zeichen");
		}
	});

	// image-upload
	edit_winning_notification_form.on("click", ".notification_file_upload", function () {
		var attrid = $(this).attr("id");
		var did = attrid.substring("notification_file_upload".length, attrid.indexOf("-"));
		$.get("/winningnotification/image/form/get/" + did, function (html) {
			objDialog.buttonedWin2(function () {
				objForm.unsetErrors("#notification_image_upload_form");
				var refreshUploadList = function () {
					location.reload(true);
				};
				objForm.submitFormWithSuccessCallback("#notification_image_upload_form", refreshUploadList, "Die Datei wurde gespeichert", "prize_upload_message");
			}, html, "Fileupload", "Hochladen", "Schliessen");
		});
	});

	edit_winning_notification_form.on("click", ".delete_prize_from_existing_base", function () {
		var count = edit_winning_notification_form.find('[id^="prize_"]').size();
		var obj = $(this);
		var id = obj.attr("id");
		var idsuffix = id.substring(id.lastIndexOf("_") + 1);
		var tr = $(this).closest("tr");
		if (1 < count) {
			$.post("/prize/delete/" + idsuffix + "/" + $("#winning_notification_form_did").val(), function (json) {
				if ("ok" == json.status) tr.remove();
				else $("#message").removeClass("hidden").html(json.msg);
			}, "json");
		}
	});
});