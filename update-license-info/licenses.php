<?php

// Quick hack to extract default license list from UploadWizard configuration.
// In future, try to export this info via the API on wiki so we can pull dynamically.
//
// Brion Vibber <brion@pobox.com>
// 2013-09-30

require 'include-stubs.php';
$config = require "mediawiki-extensions-UploadWizard/UploadWizard.config.php";
require "mediawiki-extensions-UploadWizard/UploadWizard.i18n.php";
$licenseList = array();

foreach ( $config['licenses'] as $key => $license ) {
	// Determine template -> license mappings
	if ( isset( $license['templates'] ) ) {
		$templates = $license['templates'];
	} else {
		$templates = array( $key );
	}
	
	if ( count( $templates ) < 1 ) {
		throw new Exception("No templates for $key, this is wrong.");
	}
	if ( count( $templates ) > 1 ) {
		//echo "Skipping multi-template license: $key\n";
		continue;
	}
	$template = $templates[0];
	if ( preg_match( '/^subst:/i', $template ) ) {
		//echo "Skipping subst license: $key\n";
		continue;
	}

	$msg = $messages['en'][$license['msg']];

	$licenseInfo = array(
		'desc' => $msg,
		'template' => $template
	);
	if ( isset( $license['url'] ) ) {
		$url = $license['url'];
		if ( substr( $url, 0, 2 ) == '//' ) {
			$url = 'https:' . $url;
		}
		if ( isset( $license['languageCodePrefix'] ) ) {
			$url .= $license['languageCodePrefix'] . '$lang';
		}
		$licenseInfo['url'] = $url;
	}
	$licenseList[$key] = $licenseInfo;
}

//var_dump( $licenseList );

echo "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n";
echo "<licenses xmlns=\"https://www.mediawiki.org/wiki/Extension:UploadWizard/xmlns/licenses\">\n";
foreach( $licenseList as $key => $licenseInfo ) {
	$encId = htmlspecialchars( $key );
	echo "  <license id=\"$encId\"";
	$encTemplate = htmlspecialchars( $licenseInfo['template'] );
	echo " template=\"$encTemplate\"";
	if ( isset( $licenseInfo['url'] ) ) {
		$encUrl = htmlspecialchars( $licenseInfo['url'] );
		echo " url=\"$encUrl\"";
	}
	echo "/>\n";
	
}
echo "</licenses>\n";
	