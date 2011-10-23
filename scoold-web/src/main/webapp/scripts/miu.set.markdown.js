/*
# markItUp!

Copyright (C) 2008 Jay Salvat
http://markitup.jaysalvat.com/

## Markup language: Markdown <http://daringfireball.net/projects/markdown/>

## Description
 - A basic Markdown markup set with Headings, Bold, Italic, Picture, Link, List, Quotes, Code, Preview button.
 - Feel free to add more tags.

## Install
 1. Download the zip file
 2. Unzip it in your markItUp! sets folder
 3. Modify your JS link to point at this set.js
*/

miu_set_markdown = {
	nameSpace:		"markdown", // Useful to prevent multi-instances CSS conflict
	previewParserPath:	'',
	onShiftEnter:		{keepDefault:false, openWith:'\n\n'},
	markupSet: [
		{name:'First Level Heading', key:'1', placeHolder:'Your title here...', closeWith:function(markItUp) { return miu.markdownTitle(markItUp, '=') }, className:'miu-btn-h1'},
		{name:'Second Level Heading', key:'2', placeHolder:'Your title here...', closeWith:function(markItUp) { return miu.markdownTitle(markItUp, '-') }, className:'miu-btn-h2'},
		{name:'Heading 3', key:'3', openWith:'### ', placeHolder:'Your title here...', className:'miu-btn-h3'},
		{name:'Heading 4', key:'4', openWith:'#### ', placeHolder:'Your title here...', className:'miu-btn-h4'},
		{name:'Heading 5', key:'5', openWith:'##### ', placeHolder:'Your title here...', className:'miu-btn-h5'},
		{name:'Heading 6', key:'6', openWith:'###### ', placeHolder:'Your title here...', className:'miu-btn-h6'},
		{separator:'|' },
		{name:'Bold', key:'B', openWith:'**', closeWith:'**', className:'miu-btn-strong'},
		{name:'Italic', key:'I', openWith:'_', closeWith:'_', className:'miu-btn-em'},
		{separator:'|'},
		{name:'Bullet List', openWith:'- ', className:'miu-btn-ul'},
		{name:'Numbered List', openWith:function(markItUp) {
			return markItUp.line+'. ';
		}, className:'miu-btn-ol'},
		{separator:'|'},
		{name:'Picture', key:'P', replaceWith:'![[![Alternative text]!]]([![Url:!:http://]!] "[![Title]!]")', className:'miu-btn-img'},
		{name:'Link', key:'L', openWith:'[', closeWith:']([![Url:!:http://]!] "[![Title]!]")', placeHolder:'Your text to link here...', className:'miu-btn-a'},
		{separator:'|'},
		{name:'Quote', openWith:'> ', className:'miu-btn-blockquote'},
		{name:'Code Block / Code', openWith:'(!(\t|!|`)!)', closeWith:'(!(`)!)', className:'miu-btn-code'},
	]
}

// miu namespace to avoid conflict.
miu = {
	markdownTitle: function(markItUp, c) {
		heading = '';
		n = $.trim(markItUp.selection||markItUp.placeHolder).length;
		for(i = 0; i < n; i++) {
			heading += c;
		}
		heading = '\n'+heading;
		return heading;
	}
}