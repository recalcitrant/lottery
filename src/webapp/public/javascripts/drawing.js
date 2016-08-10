$(function () {

	$.datepicker.setDefaults($.datepicker.regional['de']);
	$("#drawing_date").datepicker($.datepicker.regional["de"]);
	$("#drawing_date_next").datepicker($.datepicker.regional["de"]);
	$("#drawing_date_publish_next").datepicker($.datepicker.regional["de"]);

	$('#date_winning_notification').datetimepicker(
		{
			closeText: "OK",
			currentText: "Jetzt",
			showMinute: false,
			timeFormat: 'hh:mm',
			timeText: 'Zeit',
			hourText: 'Stunde',
			stepHour: 1
		});

	$('.digits_winning_notification').datetimepicker(
		{
			closeText: "OK",
			currentText: "Jetzt",
			showMinute: false,
			timeFormat: 'hh:mm',
			timeText: 'Zeit',
			hourText: 'Stunde',
			stepHour: 1
		});

	$('.rm_drawing').qtip({content: {text: "Löschen"}});
	$('.send_mails').qtip({content: {text: "Mailversand starten"}});
	var edit_drawing = $('.edit_drawing');
	var edit_winning_notification_email = $('.edit_winning_notification_email');
	edit_drawing.qtip({content: {text: "Ziehung editieren"}});
	edit_winning_notification_email.qtip({content: {text: "Gewinnbenachrichtigung editieren"}});
	$('.info').qtip({content: {text: "Sie müssen das Datum nicht angeben, wenn der Mailversand unmittelbar nach der Freigabe erfolgen soll"}});
	$('.edit_prize_count').qtip({content: {text: "Gewinnanzahl"}});
	$('.upload_tickets').qtip({content: {text: "Losdatei hochladen"}});
	$('.notification_history').qtip({content: {text: "Logdatei des Mailversands"}});
	$('.download_tickets').qtip({
		content: {text: "Die für diese Ziehung gespeicherten Lose als PDF herunterladen"},
		position: {my: 'top center', at: 'bottom center', target: ".download_tickets"},
		style: {tip: {border: 0, width: 1, height: 1}}});

	$('.edit_workflow').each(function () {
		var link = $(this);
		var attrid = link.attr("id");
		var did = attrid.substring("edit_workflow_".length, attrid.indexOf("-"));
		var hid = attrid.substring(attrid.indexOf("-") + 1);
		link.qtip({
			content: {text: "Status",
				ajax: {
					url: "/workflow/history/" + did + "/" + hid,
					type: "GET",
					success: function (data) {
						this.set('content.text', data);
					}
				}},
			position: {my: 'top center', at: 'bottom center', target: ".edit_workflow"},
			style: {tip: {border: 0, width: 1, height: 1}}})
	});

	$('.show_drawing').each(function () {
		var link = $(this);
		var id = link.attr("id").substring(link.attr("id").indexOf("_") + 1);
		link.qtip({
			content: {
				text: "Loading...",
				ajax: {
					url: "/drawing/get/" + id,
					type: "GET",
					success: function (data, status) {
						this.set('content.text', data);
						$('.popup_material_prize').each(function () {
							// prize_@dprize.prize.get.id.get-@drawing.dbase.lottery.id.get
							var mat = $(this);
							var attrid = mat.attr("id");
							var id = attrid.substring(attrid.indexOf("_") + 1, attrid.indexOf("-"));
							var lid = attrid.substring(attrid.indexOf("-") + 1);
							mat.qtip({
								content: {
									text: "Loading...",
									ajax: {
										url: "/drawingbase/prize/" + id + "/" + lid,
										type: "GET",
										success: function (html, status) {
											this.set('content.text', html);
										}
									}
								},
								position: {
									my: 'top center',
									at: 'bottom center',
									target: mat
								},
								hide: {
									fixed: true,
									delay: 100
								}
							});
						});
					}
				}
			},
			hide: {
				fixed: true,
				delay: 500
			}

		});
	});

	$('.add_drawing_base').click(function () {
		window.location.href = $("select[id='drawing_type_select']").val();
	});

	edit_drawing.click(function () {
        var id = $(this).attr("id");
        var did = id.substring("edit_drawing_".length, id.indexOf("-"));
        var hid = id.substring(id.indexOf("-") + 1);
        window.location.href = "/drawing/edit/" + did + "/" + hid;
    });

	edit_winning_notification_email.click(function () {
		var id = $(this).attr("id");
		var did = id.substring("edit_winning_notification_email".length, id.indexOf("-"));
		var hid = id.substring(id.indexOf("-") + 1);
		window.location.href = "/winningnotification/get/" + did + "/" + hid;
	});

	$('div').on("click", ".rm_drawing", function () {
		var id = $(this).attr("id");
		var did = id.substring("rm_drawing_".length, id.indexOf("-"));
		var hid = id.substring(id.indexOf("-") + 1);
		var del = function () {
			$.post("/drawing/delete/" + did + "/" + hid, function (json) {
				if ("ok" == json.status) window.location.href = "/drawing/list/" + json.msg;
				else $("#message").removeClass("hidden").html(json.msg);
			}, "json");
		};
		objDialog.buttonedWin2(function () {
			del();
		}, "Ziehung löschen?", "Ziehung löschen", "löschen", "abbrechen", 300, 225);
	});

	var drawings_list = $("#drawings_list tbody");
	drawings_list.on("click", ".add_prize_to_new_drawing", function () {
		drawing.cloneNew($(this).closest("tr"));
	});

	drawings_list.on("click", ".add_prize_to_existing_drawing", function () {
		drawing.cloneExisting($(this).closest("tr"));
	});

	drawings_list.on("click", ".delete_prize_from_new_drawing", function () {
		$(this).closest("tr").remove();
	});

	drawings_list.on("click", ".delete_prize_from_existing_drawing", function () {
		var id = $(this).attr("id");
		var dpid = id.substring(id.lastIndexOf("-") + 1);
		var tr = $(this).closest("tr");
		$.post("/drawingprize/delete/" + dpid, function (json) {
			if ("ok" == json.status) tr.remove();
			else $("#message").removeClass("hidden").html(json.msg);
		}, "json");
	});

	edit_drawing.click(function () {
		var id = $(this).attr("id");
		var did = id.substring("edit_drawing_".length, id.indexOf("-"));
		var hid = id.substring(id.indexOf("-") + 1);
		window.location.href = "/drawing/edit/" + did + "/" + hid;
	});
})
;