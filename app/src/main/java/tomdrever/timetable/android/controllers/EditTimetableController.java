package tomdrever.timetable.android.controllers;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.Toast;

import com.bluelinelabs.conductor.RouterTransaction;
import com.bluelinelabs.conductor.changehandler.FadeChangeHandler;

import java.util.ArrayList;

import butterknife.BindView;
import tomdrever.timetable.R;
import tomdrever.timetable.android.controllers.base.BaseController;
import tomdrever.timetable.android.views.ExpandableGridView;
import tomdrever.timetable.data.Day;
import tomdrever.timetable.data.Timetable;
import tomdrever.timetable.utils.CollectionUtils;
import tomdrever.timetable.utils.DragShadowBuilder;
import tomdrever.timetable.utils.ViewUtils;

public class EditTimetableController extends BaseController implements View.OnDragListener {

    private Timetable timetable;

    private ArrayList<Day> days;

    @BindView(R.id.edit_timetable_name) EditText nameEditText;
    @BindView(R.id.edit_timetable_description) EditText descriptionEditText;

    @BindView(R.id.name_input_layout) TextInputLayout nameTextInputLayout;
    @BindView(R.id.description_input_layout) TextInputLayout descriptionTextInputLayout;

    @BindView(R.id.edit_timetable_days) ExpandableGridView daysGridView;

    private DayGridAdapter dayGridAdapter;

    private int viewDayPosition;

    public EditTimetableController() { }

    public EditTimetableController(int index) {
        // Is new timetable!
        timetable = new Timetable();
        timetable.setIndex(index);

        days = CollectionUtils.copy(timetable.getDays());

        this.viewDayPosition = 0;
    }

    public EditTimetableController(Timetable timetable, int viewDayPosition) {
        this.timetable = timetable;
        days = CollectionUtils.copy(timetable.getDays());

        this.viewDayPosition = viewDayPosition;
    }

    private void setupActionbar() {
        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View v = inflater.inflate(R.layout.edit_day_actionbar_delete, null);
        v.setOnDragListener(this);

        getActionBar().setDisplayShowCustomEnabled(false);
        getActionBar().setCustomView(v);
    }

    @Override
    public boolean handleBack() {
        String name = nameEditText.getText().toString().trim();
        String description = descriptionEditText.getText().toString().trim();

        final Timetable newTimetable = newTimetable(name, description, days);

        if (!newTimetable.equals(timetable)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

            builder.setTitle("Discard changes?");

            // region Buttons

            // Add the buttons
            builder.setPositiveButton("Discard", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // Discard changes
                    if (newTimetable.isEmpty()) {
                        getRouter().pushController(RouterTransaction.with(new TimetableListController())
                                .popChangeHandler(new FadeChangeHandler())
                                .pushChangeHandler(new FadeChangeHandler()));
                    } else {
                        getRouter().pushController(RouterTransaction.with(new ViewTimetableController(timetable))
                                .popChangeHandler(new FadeChangeHandler())
                                .pushChangeHandler(new FadeChangeHandler()));
                    }
                }
            });

            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {

                }
            });

            // endregion

            // Create the AlertDialog
            AlertDialog dialog = builder.create();
            dialog.show();
        } else {
            if (newTimetable.isEmpty()) {
                getRouter().pushController(RouterTransaction.with(new TimetableListController())
                        .popChangeHandler(new FadeChangeHandler())
                        .pushChangeHandler(new FadeChangeHandler()));

                return true;
            }

            ViewTimetableController controller = new ViewTimetableController(timetable, viewDayPosition);

            getRouter().pushController(RouterTransaction.with(controller)
                    .popChangeHandler(new FadeChangeHandler())
                    .pushChangeHandler(new FadeChangeHandler()));
        }

        return true;
    }

    @Override
    protected View inflateView(@NonNull LayoutInflater inflater, @NonNull ViewGroup container) {
        return inflater.inflate(R.layout.controller_edit_timetable, container, false);
    }

    @Override
    protected void onViewBound(@NonNull View view) {
        super.onViewBound(view);

        setHasOptionsMenu(true);

        setupActionbar();

        nameEditText.setText(timetable.getName() != null ? timetable.getName() : "");
        nameTextInputLayout.setHint(getActivity().getString(R.string.edit_timetable_name));

        descriptionEditText.setText(timetable.getDescription() != null ? timetable.getDescription() : "");
        descriptionTextInputLayout.setHint(getActivity().getString(R.string.edit_timetable_description));

        dayGridAdapter = new DayGridAdapter(getActivity());

        daysGridView.setAdapter(dayGridAdapter);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_simple_edit, menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_save_edit) {
            String name = nameEditText.getText().toString().trim();

            if (name.isEmpty()) {
                Toast.makeText(getActivity(), "The timetable needs a name!", Toast.LENGTH_SHORT).show();
                return true;
            }

            String description = descriptionEditText.getText().toString().trim();

            // If this is an existing timetable, get rid of the old version before saving
            if (timetable.getName() != null) getFileManager().delete(timetable.getName());

            getFileManager().save(newTimetable(name, description, days));

            // Done - launch view of that timetable
            getRouter().pushController(RouterTransaction.with(
                    new ViewTimetableController(newTimetable(name, description, days)))
                    .popChangeHandler(new FadeChangeHandler())
                    .pushChangeHandler(new FadeChangeHandler()));

            Toast.makeText(getActivity(), name + " saved!", Toast.LENGTH_SHORT).show();

        }

        return super.onOptionsItemSelected(item);
    }

    private Timetable newTimetable(String name, String description, ArrayList<Day> days) {
        Timetable newTimetable = timetable.cloneItem();
        newTimetable.setName(name);
        newTimetable.setDescription(description);
        newTimetable.setDays(days);

        return newTimetable;
    }

    @Override
    protected boolean showUpNavigation() {
        return true;
    }

    @Override
    protected String getTitle() {
        return timetable.getName() != null ? "Edit " + timetable.getName() : "Create Timetable";
    }

    @Override
    public boolean onDrag(View view, DragEvent event) {
        final int action = event.getAction();

        switch(action) {

            case DragEvent.ACTION_DRAG_STARTED:
                return event.getClipDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN);

            case DragEvent.ACTION_DRAG_ENTERED: return true;

            case DragEvent.ACTION_DRAG_LOCATION: return true;

            case DragEvent.ACTION_DRAG_EXITED: return true;

            case DragEvent.ACTION_DROP:
                // Gets the timetable containing the dragged data
                ClipData.Item item = event.getClipData().getItemAt(0);

                final int position = Integer.valueOf(item.getText().toString());

                final Day day = days.get(position);

                if (view.getId() == R.id.circle_item_base) {
                    final int targetPosition = (int) view.getTag();

                    // NOTE - no need to do anything if the day hasn't been moved
                    if (position == targetPosition) return true;

                    if (position < targetPosition) {
                        for (int i = position; i < targetPosition; i++) {
                            swap(i, i + 1);
                        }
                    } else {
                        for (int i = position; i > targetPosition; i--) {
                            swap(i, i - 1);
                        }
                    }

                    dayGridAdapter.notifyDataSetChanged();

                    Snackbar.make(view,
                            day.getName() + " moved from " + (position + 1) + " to " + (targetPosition + 1),
                            Snackbar.LENGTH_SHORT).setAction("Undo", new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (targetPosition < position) {
                                for (int i = targetPosition; i < position; i++) {
                                    swap(i, i + 1);
                                }
                            } else {
                                for (int i = targetPosition; i > position; i--) {
                                    swap(i, i - 1);
                                }
                            }

                            dayGridAdapter.notifyDataSetChanged();
                        }
                    }).show();

                } else if (view.getId() == R.id.delete_day) {
                    days.remove(position);
                    dayGridAdapter.notifyDataSetChanged();

                    Snackbar.make(view, day.getName() + " deleted", Snackbar.LENGTH_SHORT).setAction("Undo", new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            days.add(position, day);
                            dayGridAdapter.notifyDataSetChanged();
                        }
                    }).show();
                }

                return true;

            case DragEvent.ACTION_DRAG_ENDED:
                showDeleteDayIcon(false);
                return true;
        }

        return false;
    }

    private class DayGridAdapter extends BaseAdapter {

        private Context context;

        DayGridAdapter(Context context) {
            this.context = context;
        }

        @Override
        public int getCount() {
            return days.size() + 1;
        }

        @Override
        public Day getItem(int i) {
            return days.get(i);
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            final View itemView;

            if (position != days.size()) {
                String text = String.valueOf(getItem(position).getName().toUpperCase().charAt(0));

                // TODO - replave with: int color = getItem(position).getColor()
                int color = ContextCompat.getColor(getApplicationContext(), R.color.blue);

                itemView = ViewUtils.createCircleView(inflater, text, color);

                // NOTE - for all items bar the last, which is the "add new" button
                itemView.setTag(position);

                // region OnClick
                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        getRouter().pushController(RouterTransaction.with(new EditDayController(days.get(position)))
                                .popChangeHandler(new FadeChangeHandler())
                                .pushChangeHandler(new FadeChangeHandler()));
                    }
                });
                // endregion

                // region OnLongClick
                itemView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View view) {
                        // hide toolbar and title
                        showDeleteDayIcon(true);

                        ClipData.Item item = new ClipData.Item(String.valueOf(itemView.getTag()));

                        ClipData dragData = new ClipData(String.valueOf(itemView.getTag()),
                                new String[] { ClipDescription.MIMETYPE_TEXT_PLAIN }, item);

                        View.DragShadowBuilder shadowBuilder = new DragShadowBuilder(itemView);

                        // Starts the drag
                        itemView.startDrag(dragData,  // the data to be dragged
                                shadowBuilder,  // the drag shadow builder
                                null,      // no need to use local data
                                0          // flags (not currently used, set to 0)
                        );

                        return true;
                    }
                });
                // endregion

                itemView.setOnDragListener(EditTimetableController.this);
            } else {
                // NOTE - for the "add new button"
                itemView = ViewUtils.createCircleView(inflater, "+",
                        ContextCompat.getColor(context, R.color.red));

                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        final Day newDay = new Day();

                        EditDayController editDayController = new EditDayController(newDay);
                        editDayController.setEditingFinishedListener(new EditingFinishedListener() {
                            @Override
                            public void onEditingFinished() {
                                days.add(newDay);
                            }
                        });

                        getRouter().pushController(RouterTransaction.with(editDayController)
                                .popChangeHandler(new FadeChangeHandler())
                                .pushChangeHandler(new FadeChangeHandler()));
                    }
                });
            }

            return itemView;
        }
    }

    private void swap(int position1, int position2) {
        Day day1 = days.get(position1);
        Day day2 = days.get(position2);

        days.set(position1, day2);
        days.set(position2, day1);
    }

    private void showDeleteDayIcon(boolean showDelete) {
        getActionBar().setDisplayHomeAsUpEnabled(!showDelete);
        getActionBar().setDisplayShowTitleEnabled(!showDelete);

        setOptionsMenuHidden(showDelete);
        getActionBar().setDisplayShowCustomEnabled(showDelete);
    }
}
