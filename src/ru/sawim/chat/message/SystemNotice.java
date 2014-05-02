
package ru.sawim.chat.message;

import protocol.Protocol;
import ru.sawim.R;
import ru.sawim.SawimApplication;
import ru.sawim.comm.JLocale;
import ru.sawim.comm.StringConvertor;

public class SystemNotice extends Message {

    public static final byte SYS_NOTICE_PRESENCE = 10;
    public static final int SYS_NOTICE_AUTHREQ = 1;
    public static final int SYS_NOTICE_ERROR = 2;
    public static final int SYS_NOTICE_MESSAGE = 3;

    private int sysnotetype;
    private String reason;

    public SystemNotice(Protocol protocol, int _sysnotetype, String _uin, String _reason) {
        super(SawimApplication.getCurrentGmtTime(), protocol, _uin, true);
        sysnotetype = _sysnotetype;
        reason = StringConvertor.notNull(_reason);
    }

    private String nick;

    public SystemNotice(Protocol protocol, byte _sysnotetype, String _uin, String nick, String _reason) {
        super(SawimApplication.getCurrentGmtTime(), protocol, _uin, true);
        sysnotetype = _sysnotetype;
        this.nick = nick;
        reason = StringConvertor.notNull(_reason);
    }

    public String getName() {
        if (SYS_NOTICE_PRESENCE == getSysnoteType()) {
            return nick;
        }
        return JLocale.getString(R.string.sysnotice);
    }

    public int getSysnoteType() {
        return sysnotetype;
    }

    public String getText() {
        String text = "";
        if (SYS_NOTICE_MESSAGE == getSysnoteType()) {
            return "* " + reason;
        }
        if (SYS_NOTICE_ERROR == getSysnoteType()) {
            return reason;
        }
        if (SYS_NOTICE_AUTHREQ == getSysnoteType()) {
            text = getSndrUin() + " " + JLocale.getString(R.string.wantsyourauth);
        }
        if (StringConvertor.isEmpty(text)) {
            return reason;
        }
        text += ".";
        if (!StringConvertor.isEmpty(reason)) {
            text += "\n" + JLocale.getString(R.string.reason) + ": " + reason;
        }
        return text;
    }
}
