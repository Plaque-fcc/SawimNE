package protocol.jabber;

import java.util.List;
import java.util.Vector;
import android.support.v4.app.FragmentActivity;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import ru.sawim.models.form.VirtualListItem;
import ru.sawim.models.list.VirtualList;
import ru.sawim.models.list.VirtualListModel;
import sawim.SawimUI;
import sawim.comm.*;
import ru.sawim.Scheme;
import sawim.util.JLocale;
import ru.sawim.models.form.FormListener;
import ru.sawim.models.form.Forms;


public final class MirandaNotes {
    private static final int COMMAND_ADD    = 0;
    private static final int COMMAND_EDIT   = 1;
    private static final int COMMAND_DEL    = 2;
    private static final int MENU_COPY      = 3;
    private static final int MENU_COPY_ALL  = 4;

    private Jabber jabber;
    private Vector notes = new Vector();
	
	private VirtualList screen;
    private VirtualListModel model;

    void init(Jabber protocol) {
        screen = VirtualList.getInstance();
        model = new VirtualListModel();
        screen.setCaption(JLocale.getString("notes"));
        jabber = protocol;
        screen.setModel(model);
        screen.setBuildOptionsMenu(new VirtualList.OnBuildOptionsMenu() {
            @Override
            public void onCreateOptionsMenu(Menu menu) {
                menu.add(Menu.FIRST, COMMAND_ADD, 2, JLocale.getString("add_to_list"));
            }

            @Override
            public void onOptionsItemSelected(FragmentActivity activity, MenuItem item) {
                switch (item.getItemId()) {
                    case COMMAND_ADD: {
                        new NoteEditor(addEmptyNote()).showIt();
                        break;
                    }
                }
            }
        });
        screen.setOnBuildContextMenu(new VirtualList.OnBuildContextMenu() {
            @Override
            public void onCreateContextMenu(ContextMenu menu, int listItem) {
                menu.add(Menu.FIRST, COMMAND_EDIT, 2, JLocale.getString("edit"));
                menu.add(Menu.FIRST, COMMAND_DEL, 2, JLocale.getString("delete"));
                menu.add(Menu.FIRST, MENU_COPY, 2, JLocale.getString("copy_text"));
                menu.add(Menu.FIRST, MENU_COPY_ALL, 2, JLocale.getString("copy_all_text"));
            }

            @Override
            public void onContextItemSelected(int listItem, int itemMenuId) {
                switch (itemMenuId) {
                    case COMMAND_EDIT: {
                        Note note = (Note)notes.elementAt(listItem);
                        new NoteEditor(note).showIt();
                        break;
                    }

                    case COMMAND_DEL:
                        removeNote(listItem);
                        jabber.getConnection().saveMirandaNotes(getNotesStorage());
                        refresh();
                        break;

                    case MENU_COPY:
                        VirtualListItem item = screen.getModel().elements.get(listItem);
                        SawimUI.setClipBoardText(item.getLabel() + "\n" + item.getDescStr());
                        break;

                    case MENU_COPY_ALL:
                        StringBuffer s = new StringBuffer();
                        List<VirtualListItem> listItems = screen.getModel().elements;
                        for (int i = 0; i < listItems.size(); ++i) {
                            s.append(listItems.get(i).getLabel()).append("\n")
                                    .append(listItems.get(i).getDescStr()).append("\n");
                        }
                        SawimUI.setClipBoardText(s.toString());
                        break;
                }
            }
        });
        screen.setClickListListener(new VirtualList.OnClickListListener() {
            @Override
            public void itemSelected(int position) {
                Note note = (Note) notes.elementAt(position);
                new NoteEditor(note).showIt();
            }

            @Override
            public boolean back() {
                screen.clearAll();
                return true;
            }
        });
    }
	
    public void clear() {
        model.clear();
	}
    public void showIt() {
        clear();
        notes.removeAllElements();
        VirtualListItem wait = model.createNewParser(true);
        wait.addDescription(JLocale.getString("wait"),
                Scheme.THEME_TEXT, Scheme.FONT_STYLE_PLAIN);
        model.addPar(wait);
        jabber.getConnection().requestMirandaNotes();
        screen.show();
    }

    void addNote(String title, String tags, String text) {
        Note note = new Note();
        note.title = title;
        note.tags = tags;
        note.text = text;
        notes.addElement(note);
        addNote(note);
    }
    private void addNote(Note note) {
		VirtualListItem parser = model.createNewParser(true);
        parser.addLabel(note.title + "\n" + "*" + note.tags, Scheme.THEME_FORM_EDIT, Scheme.FONT_STYLE_BOLD);
        parser.addDescription(note.text, Scheme.THEME_TEXT, Scheme.FONT_STYLE_PLAIN);
        model.addPar(parser);
    }

    String getNotesStorage() {
        StringBuffer storage = new StringBuffer();
        int size = notes.size();
        for (int i = 0; i < size; ++i) {
            Note note = (Note)notes.elementAt(i);
            storage.append("<note tags='").append(Util.xmlEscape(note.tags)).append("'>");
            storage.append("<title>").append(Util.xmlEscape(note.title)).append("</title>");
            storage.append("<text>").append(Util.xmlEscape(note.text)).append("</text>");
            storage.append("</note>");
        }
        return storage.toString();
    }

    private void refresh() {
        int index = screen.getCurrItem();
        clear();
        int size = notes.size();
        for (int i = 0; i < size; ++i) {
            addNote((Note)notes.elementAt(i));
        }
        screen.setCurrentItemIndex(index);
    }

    public Note addEmptyNote() {
        Note note = new Note();
        notes.addElement(note);
        return note;
    }
    private void removeNote(int currItem) {
        notes.removeElementAt(currItem);
    }
    private void selectNote(Note note) {
        screen.setCurrentItemIndex(notes.indexOf(note));
    }

    public void showNoteEditor(Note n) {
        new NoteEditor(n).showIt();
    }

    public class Note {
        private String title;
        public String tags;
        public String text;
    }

    private class NoteEditor implements FormListener {
        private static final int FIELD_TITLE = 0;
        private static final int FIELD_TAGS = 1;
        private static final int FIELD_TEXT = 2;

        private Note note;

        private NoteEditor(Note note) {
            this.note = note;
        }

        private void showIt() {
            Forms form = Forms.getInstance();
            form.init("notes", this);
            form.addTextField(FIELD_TITLE, "title", note.title);
            form.addTextField(FIELD_TAGS, "tags", note.tags);
            form.addTextField(FIELD_TEXT, "text", note.text);
            form.show();
        }

        public void formAction(Forms form, boolean apply) {
            if (apply) {
                String title = form.getTextFieldValue(FIELD_TITLE);
                String tags = form.getTextFieldValue(FIELD_TAGS);
                String text = form.getTextFieldValue(FIELD_TEXT);

                if (StringConvertor.isEmpty(title) &&
                        StringConvertor.isEmpty(tags) &&
                        StringConvertor.isEmpty(text)) {
                    return;
                }

                note.title = title;
                note.tags = tags;
                note.text = text;

                refresh();
                selectNote(note);
				jabber.getConnection().saveMirandaNotes(getNotesStorage());
                form.back();
            }
        }
    }
}