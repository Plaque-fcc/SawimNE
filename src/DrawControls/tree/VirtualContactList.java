package DrawControls.tree;

import protocol.Contact;
import protocol.Group;
import protocol.Protocol;
import protocol.XStatusInfo;
import sawim.Options;
import sawim.comm.StringConvertor;
import sawim.comm.Util;

public final class VirtualContactList {

    private static boolean initialized = false;
    OnUpdateRoster onUpdateRoster;
    private boolean useGroups;
    private ContactListModel model;
    private int currentProtocol = 0;
    private int currPage;

    public VirtualContactList() {
        model = new ContactListModel(10);
        updateOption();
    }

    public ContactListModel getModel() {
        return model;
    }

    public void update(TreeNode node) {
        if (onUpdateRoster != null)
            onUpdateRoster.updateRoster();
    }

    public final void update() {
        if (onUpdateRoster != null)
            onUpdateRoster.updateRoster();
    }

    public void updateTree() {
        if (onUpdateRoster != null)
            onUpdateRoster.updateRoster();
    }

    public void updateBarProtocols() {
        if (onUpdateRoster != null)
            onUpdateRoster.updateBarProtocols();
    }

    public void setOnUpdateRoster(OnUpdateRoster l) {
        onUpdateRoster = l;
    }

    public int getCurrPage() {
        return currPage;
    }

    public void setCurrPage(int currPage) {
        this.currPage = currPage;
    }

    public void putIntoQueue(Group group) {
        if (onUpdateRoster != null)
            onUpdateRoster.putIntoQueue(group);
    }

    private Protocol getProtocol(Group g) {
        for (int i = 0; i < getModel().getProtocolCount(); ++i) {
            Protocol p = getModel().getProtocol(i);
            if (-1 != Util.getIndex(p.getGroupItems(), g)) {
                return p;
            }
        }
        return getModel().getProtocol(0);
    }

    public void updateOption() {
        useGroups = Options.getBoolean(Options.OPTION_USER_GROUPS);
        if (!initialized) {
            initialized = true;
            ContactListModel oldModel = model;
            model = new ContactListModel(10);
            for (int i = 0; i < oldModel.getProtocolCount(); ++i) {
                model.addProtocol(oldModel.getProtocol(i));
            }
        }
        model.updateOptions(this);
    }

    public void expandNodePath(TreeNode node) {
        if ((node instanceof Contact) && useGroups) {
            Contact c = (Contact) node;
            Protocol p = model.getContactProtocol(c);
            if (null != p) {
                Group group = p.getGroupById(c.getGroupId());
                if (null == group) {
                    group = p.getNotInListGroup();
                }
                model.getGroupNode(group).setExpandFlag(true);
            }
        }
    }

    public final void setActiveContact(Contact cItem) {
        if (onUpdateRoster != null)
            onUpdateRoster.setCurrentNode(cItem);
        updateTree();
    }

    public int getCurrProtocol() {
        return currentProtocol;
    }

    public void setCurrProtocol(int currentProtocol) {
        this.currentProtocol = currentProtocol;
    }

    public final Protocol getCurrentProtocol() {
        Protocol p = model.getProtocol(getCurrProtocol());
        if (model.getProtocolCount() == 0 || null == p) {
            p = model.getProtocol(0);
        }
        return p;
    }

    public String getStatusMessage(Contact contact) {
        String message;
        Protocol protocol = model.getContactProtocol(contact);
        if (protocol == null) return "";
        if (XStatusInfo.XSTATUS_NONE != contact.getXStatusIndex()) {
            message = contact.getXStatusText();
            if (!StringConvertor.isEmpty(message)) {
                return message;
            }
            message = protocol.getXStatusInfo().getName(contact.getXStatusIndex());
            if (!StringConvertor.isEmpty(message)) {
                return message;
            }
        }
        message = contact.getStatusText();
        if (!StringConvertor.isEmpty(message)) {
            return message;
        }
        String status = protocol.getStatusInfo().getName(contact.getStatusIndex());
        return (status == null) ? "" : status;
    }

    public interface OnUpdateRoster {
        void updateRoster();

        void updateBarProtocols();

        void putIntoQueue(Group g);

        void setCurrentNode(TreeNode cItem);
    }
}