var initialIntervalCheck = 500;
var carouselSpeed = 5000;
var scrollTopAnimationTime = 100;

function checkHash() {
    var hash = null;
    if (window.location.hash) {
        if (window.location.hash == "#home") {
            hash = "#home";
			enable(0);
        } else if (window.location.hash == "#information") {
            hash = "#information";
			enable(1);
        } else if (window.location.hash == "#download") {
            hash = "#download";
			enable(2);
        } else if (window.location.hash == "#faq") {
            hash = "#faq";
			enable(3);
        } else if (window.location.hash == "#contact") {
            hash = "#contact";
			enable(4);
        } else {
            hash = "#home";
			enable(0);
		}
    } else {
		hash = "#home";
		enable(0);
	}
    if (hash != null) {
        window.location.hash = hash;
    }
}

function hide(id, func) {
	$('#' + id).fadeOut("fast", function() {
		$('#btn_' + id).removeClass("active");
		func();
	});
}

function show(id) {
	$('#' + id).fadeIn('fast', function() {
		if(id != "home") {
			$('#btn_' + id).addClass("active");
			$('#icon').fadeTo("fast", 0.6);
		} else {
			$('#icon').fadeTo("fast", 1.0);
		}
	});
}

function displayContent(hide1, hide2, hide3, hide4, show1) {
	hide(hide1, function() {
		hide(hide2, function() {
			hide(hide3, function() {
				hide(hide4, function() {
					show(show1);
				});
			});
		});
	});
}

function enable(id) {
	if(id == 1) {
		window.location.hash = "information";
		displayContent("home", "download", "faq", "contact", "information");
	} else if(id == 2) {
		window.location.hash = "download";
		displayContent("home", "information", "faq", "contact", "download");
	} else if(id == 3) {
		window.location.hash = "faq";
		displayContent("home", "information", "download", "contact", "faq");
	} else if(id == 4) {
		window.location.hash = "contact";
		displayContent("home", "information", "download", "faq", "contact");
	} else {
		window.location.hash = "home";
		displayContent("information", "download", "faq", "contact", "home");
	}
	$('html, body').animate({scrollTop:0}, scrollTopAnimationTime);
}

function parseScreenshots() {
	var available = app && app.screenshots && (app.screenshots.length > 0);
	$('#screenshots_content').html('');
    if (available) {
		$('#screenshots_content').append('<div id="screenshots_carousel" class="carousel slide" data-ride="carousel">');
		$('#screenshots_carousel').append('<ol id="carousel_indicators" class="carousel-indicators">');
			var inserted = 0;
	        for (var i = 0; i < app.screenshots.length; ++i) {
	            if (app.screenshots[i].description && app.screenshots[i].url) {
					$('#carousel_indicators').append(
						'<li data-target="#screenshots_carousel" data-slide-to="' + i + '" class="' + ((inserted == 0) ? 'active' : '') + '"></li>');
					++inserted;
				}
			}
        $('#screenshots_carousel').append('</ol>');
        $('#screenshots_carousel').append('<div id="carousel_inner" class="carousel-inner">');
	        for (var i = 0; i < app.screenshots.length; ++i) {
	            if (app.screenshots[i].description && app.screenshots[i].url) {
	                $('#carousel_inner').append(
          				'<div class="item ' + ((i == 0) ? 'active' : '') + '">' +
            				'<img src="' + app.screenshots[i].url + '" alt="' + app.screenshots[i].description + '" width="100%">' +
							'<div class="carousel-caption">' +
								app.screenshots[i].description +
	      					'</div>' +
          				'</div>'
					);
	            }
	        }
		$('#screenshots_carousel').append(
			'</div>' +
        		'<a class="left carousel-control" href="#screenshots_carousel" role="button" data-slide="prev">' +
        			'<span class="glyphicon glyphicon-chevron-left"></span>' +
        		'</a>' +
        		'<a class="right carousel-control" href="#screenshots_carousel" role="button" data-slide="next">' +
        			'<span class="glyphicon glyphicon-chevron-right"></span>' +
        		'</a>');
		$('#screenshots_carousel').append('</div>');
		$('#screenshots_carousel').carousel({
			interval: carouselSpeed
		});
    } else {
		$('#screenshots_content').append(
			'<div class="alert alert-danger" role="alert">' +
				'No data available...' +
			'</div>'
		);
	}
}

function parseInformation() {
	var available = app && (app.version || app.status || app.build);
	$('#information_content').html('');
    if (available) {
		$('#information_content').append(
			'<p>' +
				'<table id="table_information" class="table table-bordered table-hover">'+
				'</table>' +
			'</p>'
		);
	    if (app.version) {
	        $('#table_information').append(
	            '<tr class="success">' +
	            	'<td><strong>' + 'Version' + '</strong></td>' +
	            	'<td>' + app.version + '</td>' +
	            '</tr>'
			);
	    }
	    if (app.status && !(app.status == '')) {
	        $('#table_information').append(
	            '<tr class="success">' +
	            	'<td><strong>' + 'Status' + '</strong></td>' +
	            	'<td>' + app.status + '</td>' +
	            '</tr>'
			);
	    }
	    if (app.build) {
	        $('#table_information').append(
	            '<tr class="success">' +
	            	'<td><strong>' + 'Build' + '</strong></td>' +
	            	'<td>' + app.build + '</td>' +
	            '</tr>'
			);
	    }
		if(app.changelog) {
			$('#table_information').append(
	            '<tr class="success">' +
	            	'<td><strong><a href="' + app.changelog + '" target="_blank">Changelog</a></strong></td>' +
	            	'<td></td>' +
	            '</tr>'
			);
		}
	} else {
		$('#information_content').append(
			'<div class="alert alert-danger" role="alert">' +
				'No data available...' +
			'</div>'
		);
	}
}

function parseFeatures() {
	var available = app && app.features;
	$('#features_content').html('');
    if (available) {
		$('#features_content').append(
			'<p>' +
				'<table id="table_features" class="table table-bordered table-hover">'+
				'</table>' +
			'</p>'
		);
        for (var i = 0; i < app.features.length; ++i) {
            if (app.features[i]) {
                $('#table_features').append(
                    '<tr class="info">' +
                    	'<td>' + app.features[i] + '</td>' +
                    '</tr>'
				);
            }
        }
    } else {
		$('#features_content').append(
			'<div class="alert alert-danger" role="alert">' +
				'No data available...' +
			'</div>'
		);
	}
}

function addDownload(btnId, location) {
	$('#' + btnId).click(function(event){
		event.preventDefault();
		window.location.href = location;
	});
}

function parseDownload() {
	var available = app && app.download && (app.download.macos || app.download.windows_setup || app.download.windows_zip || app.download.cross || app.download.android);
	$('#download_content').html('');
    if (available) {
		if (app.downloadwarning && !(app.downloadwarning == "")) {
			$('#download_content').append(
				'<div class="alert alert-danger" role="alert">' +
					app.downloadwarning +
				'</div>'
			);
        }

		if(app.version && available) {
			$('#download_content').append('<h3>Version ' + app.version + ':</h3>');
		}
		$('#download_content').append('<div id="download_content_btns"></div>');
		if(app.download.macos) {
			$('#download_content_btns').append('<div class="btn_div"><button id="btn_macosx" type="button" class="btn btn-lg btn-block btn-danger"><i class="glyphicon glyphicon-download pull-left"></i>&nbsp;Mac OS X</button></div>');
			addDownload('btn_macosx', app.download.macos);
		}
		if(app.download.windows_setup) {
			$('#download_content_btns').append('<div class="btn_div"><button id="btn_windowsSetup" type="button" class="btn btn-lg btn-block btn-primary"><i class="glyphicon glyphicon-download pull-left"></i>&nbsp;Windows (Setup)</button></div>');
			addDownload('btn_windowsSetup', app.download.windows_setup);
		}
		if(app.download.windows_zip) {
			$('#download_content_btns').append('<div class="btn_div"><button id="btn_windowsZip" type="button" class="btn btn-lg btn-block btn-info"><i class="glyphicon glyphicon-download pull-left"></i>&nbsp;Windows (zip)</button></div>');
			addDownload('btn_windowsZip', app.download.windows_zip);
		}
		if(app.download.cross) {
			$('#download_content_btns').append('<div class="btn_div"><button id="btn_cross" type="button" class="btn btn-lg btn-block btn-success"><i class="glyphicon glyphicon-download pull-left"></i>&nbsp;Cross</button></div>');
			addDownload('btn_cross', app.download.cross);
		}
		if(app.download.android) {
			$('#download_content_btns').append('<div class="btn_div"><button id="btn_android" type="button" class="btn btn-lg btn-block btn-warning"><i class="glyphicon glyphicon-download pull-left"></i>&nbsp;Android*</button>* (Android App &copy; 2014 D. Grodt)</div>');
			addDownload('btn_android', app.download.android);
		}
    } else {
		$('#download_content').append(
			'<div class="alert alert-danger" role="alert">' +
				'No data available...' +
			'</div>'
		);
	}
}

function showAllFAQ() {
	$('.panel-body-faq').show(500);
}

function hideAllFAQ() {
	$('.panel-body-faq').hide(500);
}

function parseFAQ() {
	var available = app && app.faq;
    if (available) {
		$('#faq_content').prepend('<a id="buttonShowAllFAQ" href="#">Show all FAQ</a> | <a id="buttonHideAllFAQ" href="#">Hide all FAQ</a>');
		$('#buttonShowAllFAQ').click(function(e) {
			e.preventDefault();
			showAllFAQ();
		});
		$('#buttonHideAllFAQ').click(function(e) {
			e.preventDefault();
			hideAllFAQ();
		});
		$('#faq_content_os').html('<h3>Operating Systems</h3>');
		$('#faq_content_provider').html('<h3>Email provider</h3>');
		$('#faq_content_misc').html('<h3>Misc</h3>');
		for(var i in app.faq) {
			var id = (app.faq[i].type == 'os') ? 'faq_content_os' : ((app.faq[i].type == 'provider') ? 'faq_content_provider' : 'faq_content_misc');
			$('#' + id).append(
				'<div class="panel panel-primary">' +
					'<div class="panel-heading panel-heading-faq">' +
						'<h3 class="panel-title panel-title-faq">' +
							app.faq[i].question +
						'</h3>' +
					'</div>' +
					'<div class="panel-body panel-body-faq">' +
						app.faq[i].answer +
					'</div>' +
				'</div>'
			);
		}
		$('.panel-heading-faq').click(function(){
			$(this).parent().find('.panel-body-faq').slideToggle();
		});
    } else {
		$('#faq_content').append(
			'<div class="alert alert-danger" role="alert">' +
				'No data available...' +
			'</div>'
		);
	}
}

function collapseNavbar() {
	if($('.navbar-toggle').css('display') == 'block' && !$(this).siblings().length) {
		$('.navbar-collapse').collapse('hide');
	}
}

jQuery(document).ready(function ($) {
	$('#initial_spinner').show();
	checkHash();
    interval = setInterval(
        function () {
            if (app) {
                window.clearInterval(interval);

                parseScreenshots();
                parseInformation();
                parseFeatures();
                parseDownload();
                parseFAQ();

				$('.btn_info_info').click(function(event) {
					event.preventDefault();
					enable(1);
				});
				$('.btn_info_download').click(function(event) {
					event.preventDefault();
					enable(2);
				});
				$('.btn_info_faq').click(function(event) {
					event.preventDefault();
					enable(3);
				});
				$('.btn_info_contact').click(function(event) {
					event.preventDefault();
					enable(4);
				});
				
				$('#icon').hover(function() {
					$('#icon').fadeTo("fast", 1.0);
				}, function() {
					if(window.location.hash.indexOf("home") == -1) {
						$('#icon').fadeTo("fast", 0.6);
					}
				});
				
				$('body').click(function (e) {
					collapseNavbar();
				});
				$('#icon').click(function (e) {
					collapseNavbar();
				});
				$('.navbar-collapse a').click(function (e) {
					collapseNavbar();
				});
				
				$("#initial_spinner").remove();
				$("#content").show();

                if ('onhashchange' in window) {
                    $(window).bind('hashchange', function (e) {
                        checkHash();
                    });
                }
            }
        },
        initialIntervalCheck);
});
