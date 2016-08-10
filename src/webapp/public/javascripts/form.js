var objForm = (function () {

	function submitForm(id, message, messageid) {
		messageid = typeof(messageid) != 'undefined' ? messageid : "message";
		var options = {
			success:function (json) {
				if ("ok" == json.status) {
					$("#" + messageid).removeClass("hidden displaynone none").html(message);
				} else {
					$("#" + messageid).removeClass("hidden displaynone none").html(json.msg);
					setErrors(json.errors, json.msg);
				}
			},
			error:function (json) {
				$("#message").removeClass("hidden").html(json);
			},
			dataType:"json", // 'xml', 'script', or 'json' (expected server response type)
			clearForm:false  // clear all form fields after successful submit
		};
		$(id).ajaxSubmit(options);
		// always return false to prevent standard browser submit and page navigation:
		return false;
	}

	function submitFormWithSuccessCallback(id, callback, message, messageid) {
		message = typeof(message) != 'undefined' ? message : "Das Formular wurde gespeichert";
		messageid = typeof(messageid) != 'undefined' ? messageid : "message";
		var options = {
			success:function (json) {
				if ("ok" == json.status) {
					if ($.isFunction(callback)) callback.apply();
					else $("#" + messageid).removeClass("hidden displaynone none").html(message);
				} else {
					$("#" + messageid).removeClass("hidden displaynone none").html(json.msg);
					setErrors(json.errors, json.msg);
				}
			},
			error:function (json) {
				$("#message").removeClass("hidden").html(json);
			},
			dataType:"json", // 'xml', 'script', or 'json' (expected server response type)
			clearForm:false  // clear all form fields after successful submit
		};
		$(id).ajaxSubmit(options);
		// always return false to prevent standard browser submit and page navigation:
		return false;
	}

	function setErrors(errors, msg) {
		$(".error").append(msg);
		for (var prop in errors) {
			if (errors.hasOwnProperty(prop)) {
				if ("" != prop) {
					var item = $("#" + prop);
					$.each(errors[prop], function () {
						item.addClass("border-error");
						item.before("<span class='error'><br/>" + this + "</span><br/>");
					});
				} else $(".error").append("<br/>" + errors[''][0]);
			}
		}
	}

	function unsetErrors(fid) {
		$(fid + " input").removeClass("border-error");
		$(".error").empty();
	}

	return {
		submitForm:submitForm,
		submitFormWithSuccessCallback:submitFormWithSuccessCallback,
		setErrors:setErrors,
		unsetErrors:unsetErrors
	};
})
	();