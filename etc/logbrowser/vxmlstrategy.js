var cmd = RECORD.getField("command");
var m;
switch (cmd) {
  case "execution step":
  case "cond": {
    var s = RECORD.getBytes();
    var m;
    var expr;
    if ((m = s.match(/\(\):* (.+)/)) != undefined) {
      expr = m[1];
    }
    if (expr != undefined) {

      FIELDS.put("name", cmd + " ->" + expr);

    }
    break;
  }

  case "expression": {
    var s = RECORD.getBytes();
    var m;
    var expr = undefined;
    if ((m = s.match(/\(\):* (.+)/)) != undefined) {
      expr = m[1];
    }
    if (expr != undefined) {
      if (
        expr.includes("[__.savetmpfiles]") ||
        expr.includes("[__.savetmpfilesmode]")
      ) {
        IGNORE_RECORD = true;
      } else {
        FIELDS.put("name", cmd + " ->" + expr);
      }
    }

    break;
  }

  case 'log': {
    var s = RECORD.getBytes();
    if (s.includes('---LogServerResponse')) {
      var l = '';
      if ((m = s.match(/instruction::(\S+)/)) != null && m[1] != 'null') {
        l += '|' + m[0];
      }
      if ((m = s.match(/audio_url::(\S+)/)) != null && m[1] != 'null') {
        l += '|' + m[0];
      }
      if ((m = s.match(/text::(.+)attachdata::/)) != null && !m[1].match(/null\s+$/)) {
        l += '|text::' + m[1];
      }
      if ((m = s.match(/use_asr_enhanced_model::(\S+)/)) != null && m[1] != 'null') {
        l += '|' + m[0];
      }
      if (l)
        FIELDS.put("IVRParams", l);
    }

    var param1 = RECORD.getField("param1");
    var param2 = RECORD.getField("param2");
    FIELDS.put(
      "name",
      cmd + (param1 ? " " + param1 : "") + (param2 ? " " + param2 : "")
    );
    break;
  }

  default:
    var param1 = RECORD.getField("param1");
    var param2 = RECORD.getField("param2");
    FIELDS.put(
      "name",
      cmd + (param1 ? " " + param1 : "") + (param2 ? " " + param2 : "")
    );
    break;
}