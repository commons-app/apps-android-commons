<?php

// Stubs for enough of the MediaWiki environment to run UploadWizard.config.php

global $wgFileExtensions, $wgServer, $wgScriptPath, $wgAPIModules, $wgMaxUploadSize, $wgLang, $wgMemc, $wgUploadWizardConfig;

class FakeLang {
	function getCode() {
		return 'en';
	}
}
$wgLang = new FakeLang();

function wfMemcKey() {
	return 'fake-key';
}

class FakeMemc {
	function get() {
		return array( 'en' => 'English' );
	}
}
$wgMemc = new FakeMemc();

class FakeMessage {
	function plain() {
		return 'stub-message-plain';
	}
	function parse() {
		return 'stub-message-parsed';
	}
}

function wfMessage() {
	return new FakeMessage();
}

/**
 * Converts shorthand byte notation to integer form
 *
 * @param $string String
 * @return Integer
 */
function wfShorthandToInteger( $string = '' ) {
	$string = trim( $string );
	if ( $string === '' ) {
		return -1;
	}
	$last = $string[strlen( $string ) - 1];
	$val = intval( $string );
	switch ( $last ) {
		case 'g':
		case 'G':
			$val *= 1024;
			// break intentionally missing
		case 'm':
		case 'M':
			$val *= 1024;
			// break intentionally missing
		case 'k':
		case 'K':
			$val *= 1024;
	}

	return $val;
}

$wgAPIModules = array();
