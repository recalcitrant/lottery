var pcount = (function () {

	var did;
	var URL;
	var SAVE_OK;
	var SAVE_NOT_OK;

	function submit() {
		objFieldError.unset("#edit_drawing_prize_count_form");
		var prizeArray = [];
		var amountObj = {};
		$("#edit_drawing_prize_count_form").find('[id^="count_"]').each(
			function (key, val) {
				var element = $(val);
				var count = $.trim(element.val());
				var cfieldid = element.attr("id");
				var pid = cfieldid.substring(cfieldid.indexOf("_") + 1);
				if ("" != count) {
					var validcount = /^\s*\d+\s*$/.test(count);
					if (!validcount) objFieldError.addError(cfieldid);
				} else count = "0";
				if (!objFieldError.hasError()) {
					var pcount = {};
					pcount.pid = pid;
					pcount.count = count;
					prizeArray.push(pcount);
				}
			});
		/*var amount = $.trim($("#prize_sum_total").val());
		if ("" !== amount) {
			if (/^[1-9]([0-9]?)+,{1}[0-9]{2}$/.test(amount)) amount = amount.replace(",", "");
			else objFieldError.addError("prize_sum_total");
		} else amount = "0";*/
		if (!objFieldError.hasError()) {
		//	amountObj.amount = amount;
			$.post(URL, {data:JSON.stringify(amountObj), prizes:JSON.stringify(prizeArray)}, function (json) {
				if ("ok" == json.status) window.location.href = "/drawing/list/" + json.msg;
				else $("#message").removeClass("hidden").html(SAVE_NOT_OK);
			}, "json");
		}
	}

	function setUrl(url) {URL = url;}

	function setDrawingId(id) {did = id;}

	function setSaveOk(ok) {SAVE_OK = ok;}

	function setSaveNotOk(nok) {SAVE_NOT_OK = nok;}

	return {
		setUrl:setUrl,
		submit:submit,
		setSaveOk:setSaveOk,
		setSaveNotOk:setSaveNotOk,
		setDrawingId:setDrawingId
	};
})();