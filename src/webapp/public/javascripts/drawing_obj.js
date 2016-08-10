var drawing = (function () {

    var did;
    var URL;
    var SAVE_OK;
    var SAVE_NOT_OK;
    var CASH;
    var MATERIAL;

    function submitNew() {
        objFieldError.unset("#add_new_drawing_form");
        //[ { "id" : 1, digits: ["42", "69"]} , repeat  ]
        var prizeArray = [];
        var dataObj = {};
        var dfrom = $.trim($("#drawing_date").val());
        dataObj.date = dfrom;
        dataObj.datenext = $.trim($("#drawing_date_next").val());
	      dataObj.datepublishnext = $.trim($("#drawing_date_publish_next").val());
        dataObj.date_winning_notification = $.trim($("#date_winning_notification").val());
        if ("" === dfrom) objFieldError.addError("drawing_date");
        $("#add_new_drawing_form").find('[id^="digits-"]').each(
            function (key, val) {
                var element = $(val);
                var digits = $.trim(element.val());
                var pfieldid = element.attr("id");
                if (pfieldid.lastIndexOf("digits_winning_notification_", 0) !== 0) {
                    var pid = pfieldid.substring(pfieldid.indexOf("-") + 1, pfieldid.lastIndexOf("-"));
                    var winning_notification = $.trim($("#" + "digits_winning_notification_" + pid).val());
                    if ("" != digits) {
                        var validTokens = /^\s*\d+\s*$/.test(digits);
                        if (!validTokens) objFieldError.addError(pfieldid);
                    }
                    if (!objFieldError.hasError()) {
                        var prize = {};
                        prize.id = pid;
                        prize.digits = digits;
                        prize.winning_notification = winning_notification;
                        prizeArray.push(prize);
                    }
                }
            });
        /*var amount = $.trim($("#prize_sum_total").val());
         if ("" !== amount) {
         if (/^[1-9]([0-9]?)+,{1}[0-9]{2}$/.test(amount)) amount = amount.replace(",", "");
         else objFieldError.addError("prize_sum_total");
         }*/
        if (!objFieldError.hasError()) {
            //dataObj.amount = amount;
            $.post(URL, {data: JSON.stringify(dataObj), prizes: JSON.stringify(prizeArray)}, function (json) {
                if ("ok" == json.status) window.location.href = "/pricecount/get/" + json.did + "/" + json.hid + "/" + json.msg;
                else $("#message").removeClass("hidden").html(json.msg);
            }, "json");
        }
    }

    function submitExisting() {
        objFieldError.unset("#edit_drawing_form");
        var prizeArray = [];
        var dataObj = {};
        dataObj.date = $.trim($("#drawing_date").val());
        dataObj.datenext = $.trim($("#drawing_date_next").val());
	      dataObj.datepublishnext = $.trim($("#drawing_date_publish_next").val());
        dataObj.date_winning_notification = $.trim($("#date_winning_notification").val());
        $("#edit_drawing_form").find('[id^="digits_"]').each(
            function (key, val) {
                var element = $(val);
                var id = element.attr("id");
                // Only handle digit fields NOT winning_notification fields:
                if (id.lastIndexOf("digits_winning_notification_", 0) !== 0) {
                    var pid = id.substring(id.indexOf("_") + 1, id.indexOf("-"));
                    var dpid = id.substring(id.indexOf("-") + 1);
                    var digits = $.trim(element.val());
                    var winning_notification = $.trim($("#" + "digits_winning_notification_" + pid + "-" + dpid).val());
                    if ("" != digits) {
                        var validTokens = /^\s*\d+\s*$/.test(digits);
                        if (!validTokens) objFieldError.addError(id);
                    }

                    if (!objFieldError.hasError()) {
                        var prize = {};
                        prize.pid = pid;
                        prize.dpid = dpid;
                        prize.digits = digits;
                        prize.winning_notification = winning_notification;
                        prizeArray.push(prize);
                    }
                }
            });
        /*var amount = $.trim($("#prize_sum_total").val());
         if ("" !== amount) {
         if (/^[1-9]([0-9]?)+,{1}[0-9]{2}$/.test(amount)) amount = amount.replace(",", "");
         else objFieldError.addError("prize_sum_total");
         }*/
        if (!objFieldError.hasError()) {
            //dataObj.amount = amount;
            $.post(URL, {data: JSON.stringify(dataObj), prizes: JSON.stringify(prizeArray)}, function (json) {
                if ("ok" == json.status) window.location.href = "/drawing/list/" + json.msg;
                else $("#message").removeClass("hidden").html(SAVE_NOT_OK);
            }, "json");
        }
    }

    function getNonExistingId() {
        var existingIds = [];
        var dtable = $("#drawings_list tbody");
        dtable.find('[id^="digits-"]').each(function () {
            var id = $(this).attr("id");
            var res = id.substring(id.lastIndexOf("-") + 1);
            existingIds.push(parseInt(res));
        });
        var newid = increment();
        while (-1 != jQuery.inArray(newid, existingIds)) {
            newid = increment();
        }
        return newid;
    }

    function cloneNew(row) {
        var newid = getNonExistingId();
        row.clone().find("input,a").each(
            function () {
                $(this).attr({
                    'id': function (_, id) {
                        var sub = id.substring(0, id.lastIndexOf("-") + 1);
                        return sub + newid;
                    }
                });
                if (0 === this.name.indexOf("digits-")) {
                    $(this).attr({
                        'name': function (_, name) {
                            var sub = name.substring(0, name.lastIndexOf("-") + 1);
                            return sub + newid;
                        }
                    });
                }
            }
        ).end().insertAfter(row);
    }

    function cloneExisting(row) {
        var delement = row.find('[id^="digits_"]');
        var digits_id = delement.attr("id");
        var pid = digits_id.substring(digits_id.indexOf("_") + 1, digits_id.indexOf("-"));
        var dpid = digits_id.substring(digits_id.indexOf("-") + 1);
        var digits_value = $.trim(delement.val());
        var fieldsValid = true;
        if ("" != digits_value) {
            var validDigits = /^\s*\d+\s*$/.test(digits_value);
            if (!validDigits) {
                fieldsValid = false;
            }
        }
        if (fieldsValid) {
            var prize = {};
            prize.pid = pid;
            prize.dpid = dpid;
            prize.digits = digits_value;
            $.post("/drawingprize/add/" + did, {prize: JSON.stringify(prize)}, function (json) {
                if ("ok" == json.status) {
                    cloneEx(row, pid + "-" + json.dpid);
                }
                else $("#message").removeClass("hidden").html(json.msg);
            }, "json");
        }
    }

    function cloneEx(row, newid) {
        row.clone().find("input,a").each(
            function (i, val) {
                var ref = $(val);
                ref.attr({
                    'id': function (_, id) {
                        return id.substring(0, id.indexOf("_") + 1) + newid;
                    }
                });
                if (0 === this.name.indexOf("digits_")) {
                    ref.attr({
                        'name': function (_, name) {
                            return name.substring(0, name.indexOf("_") + 1) + newid;
                        }
                    });
                }
            }
        ).end().insertAfter(row);
    }

    function increment() {
        if (typeof increment.counter == 'undefined') {
            // this needs to be set to one, as fields which end with 1 already exist e.g. "prize_1"
            increment.counter = 1;
        }
        return ++increment.counter;
    }

    function setUrl(url) {
        URL = url;
    }

    function setDrawingId(id) {
        did = id;
    }

    function setSaveOk(ok) {
        SAVE_OK = ok;
    }

    function setSaveNotOk(nok) {
        SAVE_NOT_OK = nok;
    }

    function setCash(cash) {
        CASH = cash;
    }

    function setMat(mat) {
        MATERIAL = mat;
    }

    return {
        setUrl: setUrl,
        submitNew: submitNew,
        submitExisting: submitExisting,
        setSaveOk: setSaveOk,
        setSaveNotOk: setSaveNotOk,
        setCash: setCash,
        setMat: setMat,
        setDrawingId: setDrawingId,
        cloneExisting: cloneExisting,
        cloneNew: cloneNew
    };
})();
