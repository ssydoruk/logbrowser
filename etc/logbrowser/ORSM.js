switch (RECORD.getField("metric")) {
  case "eval_expr": {
    var s = RECORD.getBytes();
    var m;
    var expr;
    if ((m = s.match(/expression='([^\']+)' /)) != undefined) {
      expr = m[1];
    }
    if (expr != undefined) {
      if (
        expr.startsWith("storeEvent") ||
        expr.startsWith("storeUnhandledEvent")
      ) {
        IGNORE_RECORD = true;
      } else {
        var type;
        var result;

        if ((m = s.match(/type=[']([^\']+)' /)) != undefined) {
          type = m[1];
        }
        if ((m = s.match(/result='([^\']+)' /)) != undefined) {
          result = m[1];
        }
        if (result != undefined && result.endsWith('.script.done')) {
          IGNORE_RECORD = true;
        }
        else {
          FIELDS.put("param2", "result:" + result);
          FIELDS.put("param1", "type:" + type);
          FIELDS.put("param", expr);
        }
      }
    }
    break;
  }

  case "eval_condition": {
    var s = RECORD.getBytes();
    var m;
    if ((m = s.match(/condition='(.+)' result='([^\']+)' /)) != undefined) {
      FIELDS.put("param", m[1]);
      FIELDS.put("param1", "result:" + m[2]);
    }
    break;
  }

  case "transition": {
    var s = RECORD.getBytes();
    var m;

    if ((m = s.match(/name='([^\']+)'/)) != undefined) {
      FIELDS.put("param1", {
        value: m[1],
        hidden: true
      });
    }
    if ((m = s.match(/cond='([^\']+)'/)) != undefined) {
      FIELDS.put("cond", {
        value: "cond:" + m[1],
        hidden: true
      });
    }
    if ((m = s.match(/event='([^\']+)'/)) != undefined) {
      if (!m[1].endsWith('.script.done'))
        FIELDS.put("event", "evt:" + m[1]);
    }
    if ((m = s.match(/target='([^\']+)'/)) != undefined) {
      FIELDS.put("target", "targ:" + m[1]);
    }
    break;
  }

  case "log": {
    var s = RECORD.getBytes();
    var m;
    if ((m = s.match(/expr='(.+)' lab/)) != undefined) {
      var p = m[1];
      if (
        p.startsWith("done.state.") ||
        p.startsWith("Running ") ||
        p.startsWith("RelativePathURL =") ||
        p.startsWith("Entry1.done event") ||
        p.endsWith("End of this session") ||
        p.endsWith(".script.done") ||
        p.startsWith("return values = (") ||
        p.includes("script.done event") ||
        p.includes("event received in user application thread")
      ) {
        IGNORE_RECORD = true;
      } else {
        FIELDS.put("param", p);
      }
    }
    break;
  }

  case "onentry":
  case "state_enter":
  case "state_exit":
  case "onexit": {
    var m;
    if ((m = RECORD.getBytes().match(/name='([^\']+)'/)) != undefined) {
      FIELDS.put("param", m[1]);
    }
    break;
  }

  case "event_queued":
  case "event_processed": {
    // console.log('-----'+RECORD.getField("metric"));
    var m;
    if ((m = RECORD.getBytes().match(/name='([^\']+)'/)) != undefined) {
      if (m[1].endsWith('.script.done') || m[1].search(/Entry_.+\.done/) >= 0 || m[1].search(/done.state\..+_entry/) >= 0) {
        // console.log('ignored: '+m[1]);
        IGNORE_RECORD = true;
      }
      else {
        // console.log('put: '+m[1]);
        FIELDS.put("param", m[1]);
      }
    }
    break;
  }


  case "fm_exec_error":
  case "exec_error": {
    var m;
    if ((m = RECORD.getBytes().match(/error='([^\']+)' /)) != undefined) {
      FIELDS.put("param", m[1]);
    }
    break;
  }

  default:
    break;
}
