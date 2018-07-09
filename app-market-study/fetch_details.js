#!/usr/bin/env node

'use strict';

var gplay = require('google-play-scraper').memoized();
var fs = require('fs');
var saved = 0;

var friendlyStat = function(filename, callback) {
  fs.stat(filename, function(err, stats){
    if(err) {
      callback(filename, err);
    } else {
      callback(filename, err, stats);
    }
  });
}

var friendlyDetails = function(filename) {
  console.log(filename);
  var split = filename.split('/');
  split = split[split.length - 1];
  var id = split.substring(0, split.length - 5);
  gplay.app({
    appId: id,
    throttle: 1
  }).then(function(value) {
    fs.writeFile(filename,
      JSON.stringify(value, null, 4),
      function(err) {
        if (err) {
          return console.log(err);
        }
        saved += 1;
        console.log(saved, filename + ' saved!');
      });
  }, console.log);
}

var friendlyReviews = function(filename) {
  console.log(filename);
  var split = filename.split('/');
  split = split[split.length - 1];
  var id = split.substring(0, split.length - 5);
  gplay.reviews({
    appId: id,
    sort: gplay.sort.HELPFULNESS,
    throttle: 1
  }).then(function(value) {
    fs.writeFile(filename,
      JSON.stringify(value, null, 4),
      function(err) {
        if (err) {
          return console.log(err);
        }
        saved += 1;
        console.log(saved, filename + ' saved!');
      });
  }, console.log);
}

var main = function(json_prefix, pkg_name_file, callback) {
  var lineReader = require('readline').createInterface({
    input: require('fs').createReadStream(pkg_name_file)
  });
  var count = 0;
  lineReader.on('line', function (line) {
    count += 1;
    var jsonFile = json_prefix + line + '.json';
    friendlyStat(jsonFile, function(filename, err, stat) {
      if (err == null) {
        console.log(filename + ' exists.');
        // callback(filename);
      } else if (err.code == 'ENOENT') {
        callback(filename);
      } else {
        console.log('Some other error:', err.code);
      }
    });
  });
}

main('json/watchfaces/details/', 'pkg_name_wf.txt', friendlyDetails);
main('json/apps/details/', 'pkg_name_app.txt', friendlyDetails);
// main('json/watchfaces/reviews/', 'pkg_name_wf.txt', friendlyReviews);
// main('json/apps/reviews/', 'pkg_name_app.txt', friendlyReviews);
