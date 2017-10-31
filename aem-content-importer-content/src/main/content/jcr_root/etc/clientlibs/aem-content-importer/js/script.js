$("#execute").removeAttr("disabled");

var formData = new FormData();
var docToUpload = false;

$("#execute").click(function() {
	$("#error").css("display", "none");
	$("#success").css("display", "none");
	$(this).attr("disabled", "disabled");
	$("#progress").css("display", "block");
	submitForm($("#uploadContent").attr("action"));

	return false;
});

$("#clearFile").click(function() {
	$("#fileselect").val("");
	$("#messages").css("display", "none");
	$("#messages").html("");

	$("#error").css("display", "none");
	$("#error").html("");

	$("#success").css("display", "none");
	$("#success").html("");

	$("#execute").removeAttr("disabled");

	$("#progress").css("display", "none");

	formData = new FormData();
});

function submitForm(action_url) {
	// now post a new XHR request
	var xhr = new XMLHttpRequest();
	xhr.open('POST', action_url);

	formData.append("transformer", $("#transformer").val());
	formData.append("src", $("#src").val());
	formData.append("target", $("#target").val());
	formData.append("masterFile", $("#masterFile").val());
	formData.append("customProps", $("#customProps").val());

	xhr.onreadystatechange = function() {
		if (xhr.readyState == 4 && xhr.status == 200) {
			var data = $.parseJSON(xhr.responseText);
			var result = data['error'];
			if (result == 'true') {
				
				var errorType = data['errorType'];
				
				if (errorType == 'INVALID_ZIP_FILE') {
					$("#error").html(
					"Invalid zip file format. It is expected a config file as first in the root of zip or inside a folder");
				} else {
					$("#error").html(
					"Process failed expectedly!");
				}
				
				$("#error").css("display", "block");
				$("#success").css("display", "none");
				$("#execute").removeAttr("disabled");
				$("#progress").css("display", "none");

			} else {
				$("#success")
						.html(
								"Sent it the information correctly. Workflow is about to lauch.");
				$("#success").css("display", "block");
				$("#execute").remove();
			}

		}
	}

	xhr.upload.onprogress = function(event) {
		if (event.lengthComputable) {
			var complete = (event.loaded / event.total * 100 | 0);
			progress.value = progress.innerHTML = complete;
		}
	};

	xhr.onload = function() {
		// just in case we get stuck around 99%
		progress.value = progress.innerHTML = 100;
	};

	xhr.send(formData);
}

/*
 File Drag & Drop
 */
(function() {

	// getElementById
	function $id(id) {
		return document.getElementById(id);
	}

	// file drag hover
	function FileDragHover(e) {
		e.stopPropagation();
		e.preventDefault();
		e.target.className = (e.type == "dragover" ? "hover" : "");
	}

	// file selection
	function FileSelectHandler(e) {

		// cancel event and hover styling
		FileDragHover(e);

		// fetch FileList object
		var files = e.target.files || e.dataTransfer.files;

		formData = new FormData();
		formData.append('file', files[0]);
		ParseFile(files[0]);
	}

	// output file information
	function ParseFile(file) {
		if ("application/zip" != file.type) {
			$("#error")
					.css("display", "block")
					.html(
							"The file doesn't seem to be a zip. Do you want to continue?");
			$("#messages").css("display", "none");
			$("#success").css("display", "none");
		} else {
			$("#error").css("display", "none");
		}

		var m = $id("messages");
		m.innerHTML = "<p>File added: <strong>" + file.name + "</strong> ";
		$("#messages").css("display", "block");
		$("#success").css("display", "none");

		docToUpload = true;

	}

	// initialize
	function Init() {

		var fileselect = $id("fileselect");
		var filedrag = $id("filedrag");

		// file select
		fileselect.addEventListener("change", FileSelectHandler, false);

		// is XHR2 available?
		var xhr = new XMLHttpRequest();
		if (xhr.upload) {
			// file drop
			filedrag.addEventListener("dragover", FileDragHover, false);
			filedrag.addEventListener("dragleave", FileDragHover, false);
			filedrag.addEventListener("drop", FileSelectHandler, false);
			filedrag.style.display = "block";

		}

	}

	// call initialization file
	if (window.File && window.FileList && window.FileReader) {
		Init();
	}

})();