package protocol.mrim;

import DrawControls.icons.Icon;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.SubMenu;
import sawim.Options;
import sawim.comm.StringConvertor;
import sawim.modules.DebugLog;
import protocol.Contact;
import protocol.Group;
import protocol.Protocol;
import protocol.XStatusInfo;
import ru.sawim.R;

public class MrimContact extends Contact {
    private int contactId;
    private int flags;
    private String phones;
    public static final int CONTACT_INTFLAG_NOT_AUTHORIZED = 0x0001;

    public static final int USER_MENU_SEND_SMS = 1;

    public static final int CONTACT_FLAG_INVISIBLE = 0x04;
    public static final int CONTACT_FLAG_VISIBLE   = 0x08;
    public static final int CONTACT_FLAG_IGNORE    = 0x10;

    public void init(int contactId, String name, String phone, int groupId, int serverFlags, int flags) {
        setContactId(contactId);
        setName(name.length() > 0 ? name : userId);
        setGroupId(groupId);
        setFlags(flags);
        this.phones = phone;
        setBooleanValue(Contact.CONTACT_NO_AUTH, (CONTACT_INTFLAG_NOT_AUTHORIZED & serverFlags) != 0);
        setTempFlag(false);
        setOfflineStatus();
    }
    final void setFlags(int flags) {
        this.flags = flags;
        setBooleanValue(SL_VISIBLE, (flags & CONTACT_FLAG_VISIBLE) != 0);
        setBooleanValue(SL_INVISIBLE, (flags & CONTACT_FLAG_INVISIBLE) != 0);
        setBooleanValue(SL_IGNORE, (flags & CONTACT_FLAG_IGNORE) != 0);
    }
    
    public MrimContact(String uin, String name) {
        this.userId = uin;
        contactId = -1;
        setFlags(0);
        setGroupId(Group.NOT_IN_GROUP);
        this.setName(name);
        setOfflineStatus();
    }
    void setContactId(int id) {
        contactId = id;
    }
    int getContactId() {
        return contactId;
    }

    int getFlags() {
        return flags;
    }

    public void setClient(String cl) {
        DebugLog.println("client " + userId + " " + cl);
        MrimClient.createClient(this, cl);
    }
    
    public void getLeftIcons(Icon[] leftIcons) {
        super.getLeftIcons(leftIcons);
        if (!isTyping() && !hasUnreadMessage()) {
            Icon x = leftIcons[1];
            if (null != x) {
                leftIcons[0] = x;
                leftIcons[1] = null;
            }
        }
    }

    public void addChatMenuItems(ContextMenu model) {
        if (isOnline() && Options.getBoolean(Options.OPTION_ALARM)) {
            model.add(Menu.FIRST, USER_MENU_WAKE, 2, R.string.wake);
        }
    }
    protected void initContextMenu(Protocol protocol, ContextMenu contactMenu) {
        addChatItems(contactMenu);
        if (!StringConvertor.isEmpty(phones)) {
            contactMenu.add(Menu.FIRST, USER_MENU_SEND_SMS, 2, R.string.send_sms);
        }
        addGeneralItems(protocol, contactMenu);
    }
    protected void initManageContactMenu(Protocol protocol, SubMenu menu) {
        if (protocol.isConnected()) {
            initPrivacyMenu(menu);
            if (isTemp()) {
                menu.add(Menu.FIRST, USER_MENU_ADD_USER, 2, R.string.add_user);
            } else {
                if (protocol.getGroupItems().size() > 1) {
                    menu.add(Menu.FIRST, USER_MENU_MOVE, 2, R.string.move_to_group);
                }
                if (!isAuth()) {
                    menu.add(Menu.FIRST, USER_MENU_REQU_AUTH, 2, R.string.requauth);
                }
                menu.add(Menu.FIRST, USER_MENU_RENAME, 2, R.string.rename);
            }
        }
        if ((protocol.isConnected() || isTemp()) && protocol.inContactList(this)) {
            menu.add(Menu.FIRST, USER_MENU_USER_REMOVE, 2, R.string.remove);
        }
    }

    public void setMood(String moodCode, String title, String desc) {
        if (!StringConvertor.isEmpty(moodCode)) {
            DebugLog.println("mrim: mood " + getUserId() + " " + moodCode + " " + title);
        }
        String message = StringConvertor.trim(title + " " + desc);
        int x = Mrim.xStatus.createStatus(moodCode);

        setXStatus(x, message);
        if (XStatusInfo.XSTATUS_NONE == x) {
            setStatus(getStatusIndex(), message);
        }
    }

    String getPhones() {
        return phones;
    }
    void setPhones(String listOfPhones) {
        phones = listOfPhones;
    }
}