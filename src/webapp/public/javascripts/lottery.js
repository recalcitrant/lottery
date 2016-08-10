$(function () {
  $('a[id^="showlottery_"],a[id^="showlotterylong_"]').click(function () {
    var link = $(this);
    var linkid = link.attr("id");
    var index = (0 === linkid.indexOf("showlottery_")) ? "showlottery_".indexOf("_") + 1 : "showlotterylong_".indexOf("_") + 1;
    var id = linkid.substring(index);
    $.get("/lottery/" + id, function (data) {
      objDialog.dialogue(data, 400, 300);
    });
  });
});