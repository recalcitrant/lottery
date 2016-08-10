$(function () {
  $.ajaxSetup({
    cache: false
  });
  objDialog.init();
});

var objDialog = (function () {

  var buttonWin;
  var dialog;

  function init() {
    buttonWin = typeof(buttonWin) != 'undefined' ? buttonWin : $(document.createElement('div'));
    dialog = typeof(dialog ) != 'undefined' ? dialog : $(document.createElement('div'));
  }

  function close() {
    buttonWin.dialog("close");
  }

  function showContent(html) {
    buttonWin.html(html);
  }

  function dialogue(content, w, h, title) {
    w = typeof(w) != 'undefined' ? w : "auto";
    h = typeof(h) != 'undefined' ? h : "auto";
    title = typeof(title) != 'undefined' ? title : "";
    dialog.html(content);
    dialog.dialog(
      {autoOpen: false, title: title, height: h,
        width: w, modal: true, closeOnEscape: true, buttons: [
        { text: "ok", id: "ok", click: function () {
          $(this).dialog("close");
          $(this).dialog("destroy").remove();
        } }
      ] });
    dialog.dialog('open');
    return true;
  }

  function largeDialogue(content) {
    dialog.html(content);
    dialog.dialog({autoOpen: false, height: 600, width: 800, title: 'info', modal: true, closeOnEscape: true, buttons: { "ok": function () {
      $(this).dialog("close");
      $(this).dialog("destroy").remove();
    } } });
    dialog.dialog('open');
    dialog.scrollTop(0);
    return true;
  }

  function buttonedWin1(callback, html, title, buttontext, w, h, redir_after_click) {
    w = typeof(w) != 'undefined' ? w : "auto";
    h = typeof(h) != 'undefined' ? h : "auto";
    title = typeof(title) != 'undefined' ? title : "Message";
    buttontext = typeof(buttontext) != 'undefined' ? buttontext : "OK";
    redir_after_click = typeof(redir_after_click) != 'undefined' ? redir_after_click : "";
    buttonWin.html(html);
    buttonWin.dialog(buttonedWin1Props(callback, title, buttontext, w, h, redir_after_click));
    buttonWin.dialog('open');
  }

  function buttonedWin1Props(callback, title, buttontext, w, h, redir_after_click) {
    return {
      autoOpen: false,
      title: title,
      modal: true,
      closeOnEscape: true,
      open: function () {
        // prevent parent-window-scrolling:
        $("body").css("overflow", "hidden");
      },
      close: function (event, ui) {
        // reset parent-window-scrolling:
        $("body").css("overflow", "auto");
        $(this).dialog("destroy").remove();
        if ("" != redir_after_click) window.location.href = redir_after_click;
      },
      width: w,
      height: h,
      autoResize: true,
      buttons: [
        {
          id: "confirm_ok",
          text: buttontext,
          click: function () {
            if ($.isFunction(callback)) callback.apply();
          }
        }
      ]
    };
  }

  function buttonedWin2(callback, html, title, oktext, canceltext, w, h, redir_after_click) {
    w = typeof(w) != 'undefined' ? w : "auto";
    h = typeof(h) != 'undefined' ? h : "auto";
    title = typeof(title) != 'undefined' ? title : "Message";
    oktext = typeof(oktext) != 'undefined' ? oktext : "OK";
    canceltext = typeof(canceltext) != 'undefined' ? canceltext : "Cancel";
    redir_after_click = typeof(redir_after_click) != 'undefined' ? redir_after_click : "";
    buttonWin.html(html);
    buttonWin.dialog(buttonedWin2Props(callback, title, oktext, canceltext, w, h, redir_after_click));
    buttonWin.dialog('open');
  }

  function buttonedWin2Props(callback, title, oktext, canceltext, w, h, redir_after_click) {
    return {
      autoOpen: false,
      title: title,
      modal: true,
      closeOnEscape: true,
      open: function () {
        // prevent parent-window-scrolling:
        $("body").css("overflow", "hidden");
      },
      close: function (event, ui) {
        // reset parent-window-scrolling:
        $("body").css("overflow", "auto");
        $(this).dialog("destroy").remove();
        if ("" != redir_after_click) window.location.href = redir_after_click;
      },
      width: w,
      height: h,
      autoResize: true,
      buttons: [
        {
          id: "confirm_ok",
          text: oktext,
          click: function () {
            if ($.isFunction(callback)) callback.apply();
          }
        },
        {
          id: "confirm_cancel",
          text: canceltext,
          click: function () {
            $(this).dialog("close");
          }
        }
      ]
    };
  }

  function buttonedWin3(html, firstText, secondText, firstCallBack, secondCallBack) {
    buttonWin.html(html);
    buttonWin.dialog(buttonedWin2Props(firstText, secondText, firstCallBack, secondCallBack));
    buttonWin.dialog('open');
  }

  function buttonedWin3Props(firstText, secondText, firstCallBack, secondCallBack) {
    return {
      autoOpen: false,
      title: "",
      modal: true,
      closeOnEscape: true,
      open: function () {
        // prevent parent-window-scrolling:
        $("body").css("overflow", "hidden");
      },
      close: function (event, ui) {
        // reset parent-window-scrolling:
        $("body").css("overflow", "auto");
        $(this).dialog("destroy").remove();
      },
      width: 800,
      height: 600,
      buttons: [
        {
          id: "confirm_ok",
          text: firstText,
          click: function () {
            if ($.isFunction(firstCallBack)) firstCallBack.apply();
          }
        },
        {
          id: "confirm_cancel",
          text: secondText,
          click: function () {
            if ($.isFunction(secondCallBack)) secondCallBack.apply();
            $(this).dialog("close");
          }
        }
      ]
    };
  }

  return {
    largeDialogue: largeDialogue,
    dialogue: dialogue,
    buttonedWin1: buttonedWin1,
    buttonedWin2: buttonedWin2,
    buttonedWin3: buttonedWin3,
    showContent: showContent,
    close: close,
    init: init
  };

})();