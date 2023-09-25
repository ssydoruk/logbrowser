
// RECORD.put("eventdesc", "+++"+RECORD.get("eventdesc"));


processRecord();

function processRecord() {

  var s = RECORD.get("eventdesc");
  var mod = RECORD.get("mod");
  var PRINTOUT = {};

  try {
    switch (mod) {

      case "log": {

        var re ;
        if ((m = s.match(/^.+(IN THE ATTACH KVPs: |FetchConfigsOnDN completed. |configuration found for agent |IN THE REST: Response is:..|IN THE HOOP: HOOP Flags:.)(\{[^']+)/s)) != undefined) {
          // RECORD.put("eventdesc", s.replace(re, "$1" + JSON.stringify(JSON.parse(m[2]), undefined, 4)));
          RECORD.put("eventdesc", br(m[1]) + printJSON(JSON.stringify(JSON.parse(m[2]), undefined, 4)));
          return;
        }
        var re = /fetch done\"({.+})\"/s;
        if ((m = s.match(re)) != undefined) {
          var s = m[1];
          PRINTOUT.detailsMessage = s.replace(/\\u000a/g, '\n').replace(/\\\"/g, '\"');
          break;
        }

        if ((m = s.match(/application thread \((.+)\)\'/)) != undefined) {
          //        var s = m[1].replace(/\\n/g, '\n').replace(/\\\"/g, '\"').replace(/\\\\\"/g, '\"');
          var s = m[1].replace(/\\n/g, '\n').replace(/\\\"/g, '\"');
          // console.log(s);
          try {
            PRINTOUT.detailsMessage = '-->' + JSON.stringify(JSON.parse(s.replace(/(\w+):/g, '\"$1\":')), undefined, 4);
          }
          catch (e) {
            console.log('error parsing: ' + e.message);
            PRINTOUT.detailsMessage = '++>' + s;
          }
          break;
        }

        re = /(^.+IN THE BUSINESS RULE.+vRequest: )(.+\})/s;
        if ((m = s.match(re)) != undefined) {
          // var el=m[2];
          // var v1=m[1];
          // var p = "<pre>"+JSON.stringify(JSON.parse(el), undefined, 4)+"</pre>";
          try {
            RECORD.put("eventdesc", br(m[1]) + printJSON(JSON.stringify(JSON.parse(m[2]), undefined, 4)));
            return;
          }
          catch (e) {
            console.log('error parsing: ' + e.message);
          }
          break;
        }
        re = /(Rule Results :[^\{]+)(\{.+\})(.+_data.data array:[^\{]+)(\{.+\})/s;
        if ((m = s.match(re)) != undefined) {
          try {
            RECORD.put("eventdesc", m[1] + JSON.stringify(JSON.parse(m[2]), undefined, 4) + m[3] + JSON.stringify(JSON.parse(m[4]), undefined, 4));

          }
          catch (e) {
            console.log('error parsing: ' + e.message);
            PRINTOUT.detailsMessage = '++>' + el;
            PRINTOUT.detailsMessage = '++>Rule Results :\n' + el +
              '\n_data.data array:\n' + dd;
          }
          break;

        }

        if ((m = s.match(/_data.data:[^\{]+(\{.+\}).+RoutingParameters:[^\{]+(\{.+\})/)) != undefined) {
          var dt = m[1];
          var rp = m[2];
          try {
            PRINTOUT.detailsMessage = '_data.data\n' + JSON.stringify(JSON.parse(dt), undefined, 4)
              + '\nRoutingParameters\n' + JSON.stringify(JSON.parse(rp), undefined, 4)
          }
          catch (e) {
            console.log('error parsing: ' + e.message);
            PRINTOUT.detailsMessage = '++>' +
              '_data.data\n' + dt
              + '\nRoutingParameters\n' + rp;
          }
          break;

        }

        else {
          // PRINTOUT.detailsMessage=JSON.stringify({"cc":"bb", "key1":"val1"});
        }

        break;
      }

      default:
        break;
    }
  }
  catch (e) {
    console.log('error parsing: ' + e.message);
  }
  if (s != null) {
    RECORD.put("eventdesc", br(s));
  }
}


function wrap(orig, tag, attr) {
  var s = "<" + tag;
  if (attr != undefined)
    s += " " + attr;
  return s + ">" + orig + "</" + tag + ">";
}

function br(orig) {
  return orig.replaceAll("\n", '<br>');
}

function printJSON(orig){
  return wrap(wrap(orig, "span", "class=\"json\""), "pre");
}
