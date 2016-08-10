var objFieldError = (function () {

	var error = false;

	function unset(formid) {
		$(formid + " input").removeClass("border-error");
		error = false;
	}

	function addError(id) {
		$("#" + id).addClass("border-error");
		$("#message").removeClass("hidden").html('Bitte beachten Sie die rot markierten Felder');
		error = true;
	}

	function hasError() {
		return error;
	}

	return {
		unset:unset,
		addError:addError,
		hasError:hasError
	};

})();