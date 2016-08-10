$(function () {
  $('[id^="adduser"]').click(function () {
    var elem = $(this).attr("id");
    var lid = elem.substring("adduser".length, elem.indexOf("-"));
    var hlid = elem.substring(elem.indexOf("-") + 1);
    $.get("/user/addnew/" + lid + "/" + hlid, function (data) {
      objDialog.buttonedWin2(function () {
        objForm.unsetErrors("#add-user-form");
        addUser();
      }, data, "Benutzer hinzufügen", "hinzufügen", "abbrechen", 300, 530);
    });
  });
});

function userEdit(id, hash, isAdmin) {
  $.get("/user/" + id + "/" + hash, function (data) {
    objDialog.buttonedWin2(function () {
      objForm.unsetErrors("#edit-user-form");
      editUser("/", isAdmin);
    }, data, "Benutzer editieren", "speichern", "abbrechen", 300, 530);
  });
}

function userRm(username, email, url) {
  objDialog.buttonedWin2(function () {
    rmUser(url);
  }, "Benutzer <span class='error'>" + username + " (" + email + ")</span> löschen?", "Benutzer löschen", "löschen", "abbrechen", 300, 225);
}

function rmUser(url) {
  $.post(url, function (json) {
    objDialog.close();
    if ("ok" == json.status) location.reload();//window.location.href = '/user/list/user.rm.ok';
    else window.location.href = '/user/list/user.rm.notok';
  }, "json")
}

function checkPermissions() {
  var perms = [];
  $('input[name^="permissions"]:checked').each(
    function (key, val) {
      perms.push($.trim($(val).val()));
    });
  return perms;
}

function editUser(returnto, mayEdit) {
  if (!mayEdit || 0 < checkPermissions().length) {
    var options = {
      success: function (json) {
        if ("ok" == json.status) {
          objDialog.close();
          location.reload();
          //window.location.href = returnto;
        } else objForm.setErrors(json.errors, json.msg);
      },
      error: function (json) {
        objDialog.dialogue(json);
      },
      dataType: "json", // 'xml', 'script', or 'json' (expected server response type)
      clearForm: false  // clear all form fields after successful submit
    };
    $('#edit-user-form').ajaxSubmit(options);
  } else {
    objFieldError.addError("permission_head");
  }
  // always return false to prevent standard browser submit and page navigation:
  return false;
}

function addUser() {
  if (0 < checkPermissions().length) {
    var options = {
      success: function (json) {
        if ("ok" == json.status) {
          objDialog.close();
          //window.location.href = '/user/list/user.add.ok';
          location.reload();
        } else objForm.setErrors(json.errors, "");
      },
      error: function (json) {
        objDialog.dialogue(json);
      },
      dataType: "json", // 'xml', 'script', or 'json' (expected server response type)
      clearForm: false     // clear all form fields after successful submit
    };
    $('#add-user-form').ajaxSubmit(options);
  } else {
    objFieldError.addError("permission_head");
  }
  // always return false to prevent standard browser submit and page navigation:
  return false;
}