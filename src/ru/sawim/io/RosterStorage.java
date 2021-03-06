package ru.sawim.io;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import protocol.Contact;
import protocol.Group;
import protocol.Protocol;
import protocol.Roster;
import protocol.StatusInfo;
import protocol.xmpp.XmppContact;
import protocol.xmpp.XmppServiceContact;
import ru.sawim.SawimApplication;
import ru.sawim.chat.Chat;
import ru.sawim.modules.DebugLog;
import ru.sawim.roster.RosterHelper;

/**
 * Created by gerc on 18.09.2014.
 */
public class RosterStorage {

    private static final String WHERE_ACC_CONTACT_ID = DatabaseHelper.ACCOUNT_ID + " = ? AND " + DatabaseHelper.CONTACT_ID + " = ?";
    private static final String WHERE_ACCOUNT_ID = DatabaseHelper.ACCOUNT_ID + " = ?";
    private static final String WHERE_CONTACT_ID = DatabaseHelper.CONTACT_ID + " = ?";
    private static final String WHERE_SUBCONTACT_RESOURCE = DatabaseHelper.CONTACT_ID + " = ? AND " +
                                                            DatabaseHelper.SUB_CONTACT_RESOURCE + " = ?";

    public static final String storeName = "roster";
    public static final String subContactsTable = "subcontacts";

    SqlAsyncTask thread = new SqlAsyncTask("RosterStorage");

    public RosterStorage() {
        final String CREATE_ROSTER_TABLE = "create table if not exists "
                + storeName + " ("
                + DatabaseHelper.ROW_AUTO_ID + " integer primary key autoincrement, "
                + DatabaseHelper.ACCOUNT_ID + " text not null, "
                + DatabaseHelper.GROUP_NAME + " text not null, "
                + DatabaseHelper.GROUP_ID + " int, "
                + DatabaseHelper.GROUP_IS_EXPAND + " int, "
                + DatabaseHelper.CONTACT_ID + " text not null, "
                + DatabaseHelper.CONTACT_NAME + " text not null, "
                + DatabaseHelper.STATUS + " int, "
                + DatabaseHelper.STATUS_TEXT + " text, "
                + DatabaseHelper.AVATAR_HASH + " text, "
                + DatabaseHelper.FIRST_SERVER_MESSAGE_ID + " text, "
                + DatabaseHelper.IS_CONFERENCE + " int, "
                + DatabaseHelper.CONFERENCE_MY_NAME + " text, "
                + DatabaseHelper.CONFERENCE_IS_AUTOJOIN + " int, "
                + DatabaseHelper.ROW_DATA + " int, "
                + DatabaseHelper.UNREAD_MESSAGES_COUNT + " int);";

        final String CREATE_SUB_CONTACTS_TABLE = "create table if not exists "
                + subContactsTable + " ("
                + DatabaseHelper.CONTACT_ID + " text not null, "
                + DatabaseHelper.SUB_CONTACT_RESOURCE + " text, "
                + DatabaseHelper.AVATAR_HASH + " text, "
                + DatabaseHelper.SUB_CONTACT_STATUS + " int, "
                + DatabaseHelper.STATUS_TEXT + " text, "
                + DatabaseHelper.SUB_CONTACT_PRIORITY + " int, "
                + DatabaseHelper.SUB_CONTACT_PRIORITY_A + " int);";

        thread.execute(new SqlAsyncTask.OnTaskListener() {
            @Override
            public void run() {
                SawimApplication.getDatabaseHelper().getWritableDatabase().execSQL(CREATE_ROSTER_TABLE);
                SawimApplication.getDatabaseHelper().getWritableDatabase().execSQL(CREATE_SUB_CONTACTS_TABLE);
            }
        });
    }

    public synchronized void load(Protocol protocol) {
        Roster roster = protocol.getRoster();
        if (roster == null) {
            roster = new Roster();
            protocol.setRoster(roster);
        }
        Cursor cursor = null;
        try {
            cursor = SawimApplication.getDatabaseHelper().getReadableDatabase().query(storeName, null, WHERE_ACCOUNT_ID,
                    new String[]{protocol.getUserId()}, null, null, null);
            if (cursor.moveToFirst()) {
                do {
                    Contact contact = getContact(protocol, cursor);
                    roster.getContactItems().put(contact.getUserId(), contact);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            DebugLog.panic(e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        protocol.setRoster(roster);
    }

    public Contact getContact(Protocol protocol, Cursor cursor) {
        String account = cursor.getString(cursor.getColumnIndex(DatabaseHelper.ACCOUNT_ID));
        String groupName = cursor.getString(cursor.getColumnIndex(DatabaseHelper.GROUP_NAME));
        int groupId = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.GROUP_ID));
        boolean groupIsExpand = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.GROUP_IS_EXPAND)) == 1;
        String userId = cursor.getString(cursor.getColumnIndex(DatabaseHelper.CONTACT_ID));
        String userName = cursor.getString(cursor.getColumnIndex(DatabaseHelper.CONTACT_NAME));
        int status = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.STATUS));
        String statusText = cursor.getString(cursor.getColumnIndex(DatabaseHelper.STATUS_TEXT));
        String avatarHash = cursor.getString(cursor.getColumnIndex(DatabaseHelper.AVATAR_HASH));
        String firstServerMsgId = cursor.getString(cursor.getColumnIndex(DatabaseHelper.FIRST_SERVER_MESSAGE_ID));

        boolean isConference = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.IS_CONFERENCE)) == 1;
        String conferenceMyNick = cursor.getString(cursor.getColumnIndex(DatabaseHelper.CONFERENCE_MY_NAME));
        boolean conferenceIsAutoJoin = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.CONFERENCE_IS_AUTOJOIN)) == 1;

        byte booleanValues = (byte) cursor.getInt(cursor.getColumnIndex(DatabaseHelper.ROW_DATA));

        Group group = protocol.getGroupItems().get(groupId);
        if (group == null) {
            group = protocol.createGroup(groupName);
            group.setExpandFlag(groupIsExpand);
            protocol.getGroupItems().put(groupId, group);
        }

        Contact contact = protocol.getItemByUID(userId);
        if (contact == null) {
            contact = protocol.createContact(userId, userName, isConference);
        }
        contact.firstServerMsgId = firstServerMsgId;
        contact.avatarHash = avatarHash;
        contact.setStatus((byte) status, statusText);
        contact.setGroupId(groupId);
        contact.setBooleanValues(booleanValues);
        if (contact instanceof XmppContact) {
            XmppContact xmppContact = (XmppContact)contact;
            loadSubContacts(xmppContact);
        }
        if (isConference) {
            XmppServiceContact serviceContact = (XmppServiceContact) contact;
            serviceContact.setMyName(conferenceMyNick);
            serviceContact.setAutoJoin(conferenceIsAutoJoin);
            serviceContact.setConference(true);
        }
        RosterHelper.getInstance().updateGroup(protocol, group);
        return contact;
    }

    public void loadSubContacts(XmppContact xmppContact) {
        Cursor cursor = null;
        try {
            cursor = SawimApplication.getDatabaseHelper().getReadableDatabase().query(subContactsTable, null, WHERE_CONTACT_ID,
                    new String[]{xmppContact.getUserId()}, null, null, null);
            if (cursor.moveToFirst()) {
                do {
                    String subcontactRes = cursor.getString(cursor.getColumnIndex(DatabaseHelper.SUB_CONTACT_RESOURCE));
                    int subcontactStatus = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.SUB_CONTACT_STATUS));
                    String statusText = cursor.getString(cursor.getColumnIndex(DatabaseHelper.STATUS_TEXT));
                    int subcontactPriority = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.SUB_CONTACT_PRIORITY));
                    int subcontactPriorityA = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.SUB_CONTACT_PRIORITY_A));
                    String avatarHash = cursor.getString(cursor.getColumnIndex(DatabaseHelper.AVATAR_HASH));
                    if (subcontactRes != null) {
                        XmppServiceContact.SubContact subContact = xmppContact.subcontacts.get(subcontactRes);
                        if (subContact == null) {
                            subContact = new XmppContact.SubContact();
                            subContact.resource = subcontactRes;
                            subContact.status = (byte) subcontactStatus;
                            subContact.statusText = statusText;
                            subContact.priority = (byte) subcontactPriority;
                            subContact.priorityA = (byte) subcontactPriorityA;
                            subContact.avatarHash = avatarHash;
                            xmppContact.subcontacts.put(subcontactRes, subContact);
                        }
                    }
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            DebugLog.panic(e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public void save(final Protocol protocol, final Contact contact, Group group) {
        if (group == null) {
            group = protocol.getNotInListGroup();
        }
        //Log.e("save", contact.getUserId()+" "+protocol.getStatusInfo().getName(contact.getStatusIndex()));
        final Group finalGroup = group;
        thread.execute(new SqlAsyncTask.OnTaskListener() {
            @Override
            public void run() {
                Cursor cursor = null;
                try {
                    SQLiteDatabase sqLiteDatabase = SawimApplication.getDatabaseHelper().getWritableDatabase();
                    cursor = sqLiteDatabase.query(storeName,
                            new String[]{DatabaseHelper.ACCOUNT_ID, DatabaseHelper.CONTACT_ID},
                            WHERE_ACC_CONTACT_ID, new String[]{protocol.getUserId(), contact.getUserId()}, null, null, null);
                    if (cursor != null && cursor.getCount() > 0) {
                        sqLiteDatabase.update(storeName, getRosterValues(protocol, finalGroup, contact),
                                WHERE_ACC_CONTACT_ID, new String[]{protocol.getUserId(), contact.getUserId()});
                    } else {
                        sqLiteDatabase.insert(storeName, null, getRosterValues(protocol, finalGroup, contact));
                    }
                } catch (Exception e) {
                    DebugLog.panic(e);
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            }
        });
    }

    public void save(final XmppContact contact, final XmppServiceContact.SubContact subContact) {
        thread.execute(new SqlAsyncTask.OnTaskListener() {
            @Override
            public void run() {
                Cursor cursor = null;
                try {
                    SQLiteDatabase sqLiteDatabase = SawimApplication.getDatabaseHelper().getWritableDatabase();
                    cursor = sqLiteDatabase.query(subContactsTable, null, WHERE_SUBCONTACT_RESOURCE,
                                                  new String[] {contact.getUserId(), subContact.resource}, null, null,
                                                  null);
                    if (cursor != null && cursor.getCount() > 0) {
                        sqLiteDatabase.update(subContactsTable, getSubContactsValues(contact, subContact),
                                WHERE_SUBCONTACT_RESOURCE,
                                new String[]{contact.getUserId(), subContact.resource});
                    } else {
                        sqLiteDatabase.insert(subContactsTable, null, getSubContactsValues(contact, subContact));
                    }
                } catch (Exception e) {
                    DebugLog.panic(e);
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            }
        });
    }

    public void delete(final Contact contact, final XmppServiceContact.SubContact subContact) {
        if (subContact == null) {
            return;
        }

        thread.execute(new SqlAsyncTask.OnTaskListener() {
            @Override
            public void run() {
                try {
                    SQLiteDatabase sqLiteDatabase = SawimApplication.getDatabaseHelper().getWritableDatabase();
                    sqLiteDatabase.delete(subContactsTable, WHERE_SUBCONTACT_RESOURCE,
                                          new String[] {contact.getUserId(), subContact.resource});
                } catch (Exception e) {
                    DebugLog.panic(e);
                }
            }
        });
    }

    public void deleteContact(final Protocol protocol, final Contact contact) {
        thread.execute(new SqlAsyncTask.OnTaskListener() {
            @Override
            public void run() {
                SQLiteDatabase sqLiteDatabase = SawimApplication.getDatabaseHelper().getWritableDatabase();
                sqLiteDatabase.delete(storeName, WHERE_ACC_CONTACT_ID, new String[]{protocol.getUserId(), contact.getUserId()});
            }
        });
    }

    public void deleteGroup(final Protocol protocol, final Group group) {
        thread.execute(new SqlAsyncTask.OnTaskListener() {
            @Override
            public void run() {
                SQLiteDatabase sqLiteDatabase = SawimApplication.getDatabaseHelper().getWritableDatabase();
                for (Contact contact : group.getContacts()) {
                    sqLiteDatabase.delete(storeName, DatabaseHelper.ACCOUNT_ID + "= ?" + " and " + DatabaseHelper.GROUP_ID + "= ?",
                            new String[]{protocol.getUserId(), String.valueOf(group.getGroupId())});
                }
            }
        });
    }

    public void updateGroup(final Protocol protocol, final Group group) {
        thread.execute(new SqlAsyncTask.OnTaskListener() {
            @Override
            public void run() {
                SQLiteDatabase sqLiteDatabase = SawimApplication.getDatabaseHelper().getWritableDatabase();
                for (Contact contact : group.getContacts()) {
                    ContentValues values = new ContentValues();
                    values.put(DatabaseHelper.GROUP_ID, group.getGroupId());
                    values.put(DatabaseHelper.GROUP_NAME, group.getName());
                    sqLiteDatabase.update(storeName, values, WHERE_ACC_CONTACT_ID, new String[]{protocol.getUserId(), contact.getUserId()});
                }
            }
        });
    }

    public void addGroup(final Protocol protocol, final Group group) {
        thread.execute(new SqlAsyncTask.OnTaskListener() {
            @Override
            public void run() {
                SQLiteDatabase sqLiteDatabase = SawimApplication.getDatabaseHelper().getWritableDatabase();
                for (Contact contact : group.getContacts()) {
                    //Log.e("addGroup", contact.getUserId()+" "+protocol.getStatusInfo().getName(contact.getStatusIndex()));
                    sqLiteDatabase.insert(storeName, null, getRosterValues(protocol, group, contact));
                }
            }
        });
    }

    public void setOfflineStatuses(final Protocol protocol) {
        thread.execute(new SqlAsyncTask.OnTaskListener() {
            @Override
            public void run() {
                SQLiteDatabase sqLiteDatabase = SawimApplication.getDatabaseHelper().getWritableDatabase();
                for (Contact contact : protocol.getContactItems().values()) {
                    ContentValues values = new ContentValues();
                    values.put(DatabaseHelper.STATUS, StatusInfo.STATUS_OFFLINE);
                    if (contact instanceof XmppContact) {
                        sqLiteDatabase.delete(
                                subContactsTable,
                                WHERE_CONTACT_ID,
                                new String[]{contact.getUserId()});
                    }
                    sqLiteDatabase.update(storeName, values, WHERE_ACC_CONTACT_ID, new String[]{protocol.getUserId(), contact.getUserId()});
                }
            }
        });
    }

    private ContentValues getRosterValues(Protocol protocol, Group group, Contact contact) {
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.ACCOUNT_ID, protocol.getUserId());
        values.put(DatabaseHelper.GROUP_ID, group.getGroupId());
        values.put(DatabaseHelper.GROUP_NAME, group.getName());
        values.put(DatabaseHelper.GROUP_IS_EXPAND, group.isExpanded() ? 1 : 0);

        values.put(DatabaseHelper.CONTACT_ID, contact.getUserId());
        values.put(DatabaseHelper.CONTACT_NAME, contact.getName());
        values.put(DatabaseHelper.STATUS, contact.getStatusIndex());
        values.put(DatabaseHelper.STATUS_TEXT, contact.getStatusText());
        values.put(DatabaseHelper.IS_CONFERENCE, contact.isConference() ? 1 : 0);
        if (contact.isConference()) {
            XmppServiceContact serviceContact = (XmppServiceContact) contact;
            values.put(DatabaseHelper.CONFERENCE_MY_NAME, serviceContact.getMyName());
            values.put(DatabaseHelper.CONFERENCE_IS_AUTOJOIN, serviceContact.isAutoJoin() ? 1 : 0);
        }
        values.put(DatabaseHelper.ROW_DATA, contact.getBooleanValues());
        return values;
    }

    public ContentValues getSubContactsValues(XmppContact contact, XmppServiceContact.SubContact subContact) {
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.CONTACT_ID, contact.getUserId());
        values.put(DatabaseHelper.SUB_CONTACT_RESOURCE, subContact.resource);
        values.put(DatabaseHelper.SUB_CONTACT_STATUS, subContact.status);
        values.put(DatabaseHelper.STATUS_TEXT, subContact.statusText);
        values.put(DatabaseHelper.SUB_CONTACT_PRIORITY, subContact.priority);
        values.put(DatabaseHelper.SUB_CONTACT_PRIORITY_A, subContact.priorityA);
        return values;
    }

    public void updateFirstServerMsgId(final Contact contact) {
        thread.execute(new SqlAsyncTask.OnTaskListener() {
            @Override
            public void run() {
                try {
                    ContentValues values = new ContentValues();
                    values.put(DatabaseHelper.FIRST_SERVER_MESSAGE_ID, contact.firstServerMsgId);
                    SawimApplication.getDatabaseHelper().getWritableDatabase().update(storeName, values, WHERE_CONTACT_ID, new String[]{contact.getUserId()});
                } catch (Exception e) {
                    DebugLog.panic(e);
                }
            }
        });
    }

    public void updateAvatarHash(final String uniqueUserId, final String hash) {
        thread.execute(new SqlAsyncTask.OnTaskListener() {
            @Override
            public void run() {
                try {
                    ContentValues values = new ContentValues();
                    values.put(DatabaseHelper.AVATAR_HASH, hash);
                    SawimApplication.getDatabaseHelper().getWritableDatabase().update(storeName, values, WHERE_CONTACT_ID, new String[]{uniqueUserId});
                } catch (Exception e) {
                    DebugLog.panic(e);
                }
            }
        });
    }

    public void updateSubContactAvatarHash(final String uniqueUserId, final String resource, final String hash) {
        thread.execute(new SqlAsyncTask.OnTaskListener() {
            @Override
            public void run() {
                try {
                    String where = WHERE_CONTACT_ID + " and "
                            + DatabaseHelper.SUB_CONTACT_RESOURCE + "= ?";
                    String[] selectionArgs = new String[]{uniqueUserId, resource};
                    ContentValues values = new ContentValues();
                    values.put(DatabaseHelper.AVATAR_HASH, hash);
                    SawimApplication.getDatabaseHelper().getWritableDatabase().update(subContactsTable, values, where, selectionArgs);
                } catch (Exception e) {
                    DebugLog.panic(e);
                }
            }
        });
    }

    public synchronized String getSubContactAvatarHash(String uniqueUserId, String resource) {
        String hash = null;
        Cursor cursor = null;
        try {
            String where = WHERE_CONTACT_ID + " and "
                    + DatabaseHelper.SUB_CONTACT_RESOURCE + "= ?";
            String[] selectionArgs = new String[]{uniqueUserId, resource};
            cursor = SawimApplication.getDatabaseHelper().getReadableDatabase().query(subContactsTable, null,
                    where, selectionArgs, null, null, null);
            if (cursor.moveToFirst()) {
                do {
                    hash = cursor.getString(cursor.getColumnIndex(DatabaseHelper.AVATAR_HASH));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            DebugLog.panic(e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return hash;
    }

    public synchronized String getAvatarHash(String uniqueUserId) {
        String hash = null;
        Cursor cursor = null;
        try {
            String where = WHERE_CONTACT_ID;
            String[] selectionArgs = new String[]{uniqueUserId};
            cursor = SawimApplication.getDatabaseHelper().getReadableDatabase().query(storeName, null,
                    where, selectionArgs, null, null, null);
            if (cursor.moveToFirst()) {
                do {
                    hash = cursor.getString(cursor.getColumnIndex(DatabaseHelper.AVATAR_HASH));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            DebugLog.panic(e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return hash;
    }

    public void updateUnreadMessagesCount(final String protocolId, final String uniqueUserId, final int count) {
        thread.execute(new SqlAsyncTask.OnTaskListener() {
            @Override
            public void run() {
                try {
                    SQLiteDatabase sqLiteDatabase = SawimApplication.getDatabaseHelper().getWritableDatabase();
                    ContentValues values = new ContentValues();
                    values.put(DatabaseHelper.UNREAD_MESSAGES_COUNT, count);
                    sqLiteDatabase.update(storeName, values, WHERE_ACC_CONTACT_ID, new String[]{protocolId, uniqueUserId});
                } catch (Exception e) {
                    DebugLog.panic(e);
                }
            }
        });
    }

    public void loadUnreadMessages() {
        thread.execute(new SqlAsyncTask.OnTaskListener() {
            @Override
            public void run() {
                Cursor cursor = null;
                try {
                    cursor = SawimApplication.getDatabaseHelper().getReadableDatabase().query(storeName, null, null, null, null, null, null);
                    if (cursor.moveToFirst()) {
                        do {
                            String account = cursor.getString(cursor.getColumnIndex(DatabaseHelper.ACCOUNT_ID));
                            String userId = cursor.getString(cursor.getColumnIndex(DatabaseHelper.CONTACT_ID));
                            short unreadMessageCount = cursor.getShort(cursor.getColumnIndex(DatabaseHelper.UNREAD_MESSAGES_COUNT));
                            boolean isConference = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.IS_CONFERENCE)) == 1;
                            if (unreadMessageCount == 0) {
                                continue;
                            }
                            Protocol protocol = RosterHelper.getInstance().getProtocol(account);
                            if (protocol != null) {
                                Contact contact = protocol.getItemByUID(userId);
                                if (contact == null) {
                                    contact = protocol.createContact(userId, userId, isConference);
                                }
                                Chat chat = protocol.getChat(contact);
                                chat.setOtherMessageCounter(unreadMessageCount);
                            }
                        } while (cursor.moveToNext());
                    }
                } catch (Exception e) {
                    DebugLog.panic(e);
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            }
        });
    }
}
