// evaluate SDP parameters
//console.info('log '+RECORD.getField('id'));
var hasBody = false;
var m;
var contentType = undefined;
var lastLine = undefined;
var dialogStart = undefined;
var conferenceID = undefined;

for (var s of RECORD.getBytes().split("\n")) {
  if (
    (m = s.match(/^content-length:\s*(.+)/i)) != undefined ||
    (m = s.match(/^l:\s*(.+)/i)) != undefined
  ) {
    if (parseInt(m[1]) > 0) {
      hasBody = true;
    }
  } else if ((m = s.match(/^content-type:\s*([^;]+)/i)) != undefined ||
    (m = s.match(/^c:\s*([^;]+)/i)) != undefined) {
    contentType = m[1].toLowerCase();
  }
  if ((m = s.match(/User-Agent:\s*(.+)/)) != undefined) {
    FIELDS.put("User-Agent (sip)", {
      value: m[1],
      hidden: true
    });
  }
  if (hasBody) {
    if (contentType != undefined) {
      if (contentType.includes('sdp')) {
        if ((m = s.match(/(m=audio.+)/)) != undefined) {
          FIELDS.put("mline", {
            value: m[1],
            hidden: true
          });
        } else if (
          (m = s.match(/a=(inactive|recvonly|sendrecv|sendonly)/)) != undefined
        ) {
          FIELDS.put("sdp attribute", m[1]); // to group better
        } else if ((m = s.match(/^(c=.+)/)) != undefined) {
          FIELDS.put("treatment", m[1]);
        }
      } else if (contentType.includes('msml')) {
        if (dialogStart == undefined) {
          if ((m = s.match(/^<(dialogstart)/i)) != undefined ||
            (m = s.match(/^<(play)/i)) != undefined
          ) {
            dialogStart = m[1];
          }
        }
        if (dialogStart != undefined) {
          if (
            (m = s.match(/(src="[^"]+")/i)) != undefined ||
            (m = s.match(/(uri="[^"]+")/i)) != undefined
          ) {
            FIELDS.put("treatment", dialogStart + ' ' + m[1]);
            break;
          }
        } else {
          if ((m = s.match(/(result response="\d+")/i)) != undefined) {
            FIELDS.put("treatment", m[1]);
          } else if ((m = s.match(/(event)\s+name="([^"]+)"/i)) != undefined) {
            FIELDS.put("treatment", m[1] + ' ' + m[2]);
          } else if ((m = s.match(/(createconference)\s+name="([^"]+)"/i)) != undefined) {
            FIELDS.put("treatment", m[1] + ' ' + m[2]);
            break;
          } else if ((m = s.match(/(modifyconference)\s+id="([^"]+)"/i)) != undefined) {
            conferenceID = m[2];
          } else if (conferenceID != undefined) {
            if ((m = s.match(/(gvp:recorder state="[^"]+")/i)) != undefined) {
              FIELDS.put("treatment", conferenceID + ' ' + m[1]);
              break;
            }
          }
        }
      } else if (contentType.includes('x-www-form-urlencoded')) {
        if (s.trim().length > 0) {
          lastLine = s;
        }
      }
    }
  }
}
if (lastLine != undefined) {
  FIELDS.put("treatment", decodeURIComponent(lastLine).replace(/&/g, ' ').replace(/\\"/g, '"'));
}