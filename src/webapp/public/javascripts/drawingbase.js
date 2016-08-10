$(function () {

  objDialog.init();

  $(document).on("click", ".rm_prize_file_upload", function () {
    // qtip does not work as we'd have to bind to a future-event here:
    //$('.rm_prize_file_upload').qtip({content:{text:"löschen"}});
    var id = $(this).attr("id");
    var uid = id.substring("rm_prize_file_upload_".length);
    $.post("/prize/rmupload/" + uid, function (json) {
      if ("ok" == json.status) $("#" + id).closest("tr").remove();
      else $("#message").removeClass("hidden").html(json.msg);
    }, "json");
  });

  $("#sortable").sortable();

  // TOOLTIPS START
  var edit_drawing_base = $('.edit_drawing_base');
  edit_drawing_base.qtip({content: {text: "Editieren"}});
  $('.edit_prize_title').qtip({content: {text: "Bezeichnung"}});
  $('.edit_prize_description').qtip({content: {text: "Beschreibung"}});
  $('.upload_prize_file').qtip({content: {text: "Fileupload"}});
  $('.rm_drawing_base').qtip({content: {text: "Löschen"}});
  $('.use').qtip({content: {text: "Für Ziehung verwenden"}});
  // TOOLTIPS END

  // DRAWING-BASE START
  $('.show_drawing_base').each(function () {
    var link = $(this);
    var id = link.attr("id");
    link.qtip({
      content: {
        text: "Loading...",
        ajax: {
          url: "/drawingbase/get/" + id,
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

  edit_drawing_base.click(function () {
    var id = $(this).attr("id");
    var baseid = id.substring("edit_dbase_".length, id.indexOf("-"));
    var hid = id.substring(id.indexOf("-") + 1);
    window.location.href = "/drawingbase/edit/" + baseid + "/" + hid;
  });

  $('div').on("click", ".rm_drawing_base", function () {
    var id = $(this).attr("id");
	  var baseid = id.substring("rm_dbase_".length, id.indexOf("-"));
	  var hid = id.substring(id.indexOf("-") + 1);
    var del = function () {
      $.post("/drawingbase/delete/" + baseid + "/" + hid, function (json) {
        if ("ok" == json.status) window.location.reload();
        else $("#message").removeClass("hidden").html(json.msg);
      }, "json");
    };
    objDialog.buttonedWin2(function () {
      del();
    }, "Ziehungsbasis \"" + $("#"+id).data(baseid) + "\" löschen?", "Ziehungsbasis löschen", "löschen", "abbrechen", 300, 225);

  });
  // DRAWING-BASE END

  // PRIZE START

  // prize-description
  var edit_drawing_base_form = $("#edit_drawing_base_form");
  edit_drawing_base_form.on("click", ".edit_prize_description", function () {
    var id = $(this).attr("id");
    var pid = id.substring(id.lastIndexOf("_") + 1);
    $.get("/prize/description/get/" + pid, function (json) {
      var html = '<textarea class="textarea_upload" id="drawing_base_prize_description_input" rows="30" cols="70">' + json + '</textarea>';
      objDialog.buttonedWin2(function () {
        var descVal = $.trim($("#drawing_base_prize_description_input").val());
        if (300 >= descVal.length) {
          $.post("/prize/description/update/" + pid, {description: descVal}, function (json) {
            $("#message").removeClass("hidden").html(json.msg);
            objDialog.close();
          }, "json");
        } else {
          $("#message").removeClass("hidden").html("Bitte beschränken Sie die Beschreibung auf 300 Zeichen");
        }
      }, html, "Beschreibung", "Speichern", "Abbrechen");
    });
  });

  // prize-title
  edit_drawing_base_form.on("click", ".edit_prize_title", function () {
    var id = $(this).attr("id");
    var pid = id.substring(id.lastIndexOf("_") + 1);
    $.get("/prize/title/get/" + pid, function (json) {
      var html = '<input id="drawing_base_prize_title_input" maxlength="255" value="' + json + '"/>';
      objDialog.buttonedWin2(function () {
        $.post("/prize/title/update/" + pid, {title: $.trim($("#drawing_base_prize_title_input").val())}, function (json) {
          $("#message").removeClass("hidden").html(json.msg);
          $("#show_prize_title_" + pid).html($("#drawing_base_prize_title_input").val());
          objDialog.close();
        }, "json");
      }, html, "Bezeichnung", "Speichern", "Abbrechen");
    });
  });

  // prize-upload
  edit_drawing_base_form.on("click", ".upload_prize_file", function () {
    var id = $(this).attr("id");
    var pid = id.substring(id.lastIndexOf("_") + 1);
    $.get("/prize/upload/" + pid, function (html) {
      objDialog.buttonedWin2(function () {
        objForm.unsetErrors("#prize_upload_form");
        var refreshUploadList = function () {
          $.get("/prize/upload/" + pid, function (result) {
            objDialog.showContent(result);
          })
        };
        $.get("/prize/upload/count/" + pid, function (json) {
          if (2 > json.count) {
            objForm.submitFormWithSuccessCallback("#prize_upload_form", refreshUploadList, "Die Datei wurde gespeichert", "prize_upload_message");
          } else {
            $("#message").removeClass("hidden").html("Sie können maximal 2 Dateien hochladen");
          }
        });
      }, html, "Fileupload", "Hochladen", "Schliessen");
    });
  });

  var add_new_drawing_base_form = $("#add_new_drawing_base_form");
  add_new_drawing_base_form.on("click", ".add_prize_to_new_base", function () {
    dbase.cloneNew($(this).closest("tr"));
  });

  edit_drawing_base_form.on("click", ".add_prize_to_existing_base", function () {
    dbase.cloneExisting($(this).closest("tr"));
  });

  add_new_drawing_base_form.on("click", ".delete_prize_from_new_base", function () {
    $(this).closest("tr").remove();
  });

  edit_drawing_base_form.on("click", ".delete_prize_from_existing_base", function () {
    var count = edit_drawing_base_form.find('[id^="prize_"]').size();
    var obj = $(this);
    var id = obj.attr("id");
    var idsuffix = id.substring(id.lastIndexOf("_") + 1);
    var tr = $(this).closest("tr");
    if (1 < count) {
      $.post("/prize/delete/" + idsuffix + "/" + $("#edit_drawing_base_form_dbid").val(), function (json) {
        if ("ok" == json.status) tr.remove();
        else $("#message").removeClass("hidden").html(json.msg);
      }, "json");
    }
  });
  // PRIZE END
});