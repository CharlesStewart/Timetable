package tomdrever.timetable.android.fragments;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.*;
import android.widget.EditText;
import android.widget.Toast;
import tomdrever.timetable.R;
import tomdrever.timetable.utility.TimetableFileManager;
import tomdrever.timetable.android.listeners.EditingFinishedListener;
import tomdrever.timetable.android.listeners.CardTouchedListener;
import tomdrever.timetable.android.adapters.DaysRecyclerViewAdapter;
import tomdrever.timetable.android.listeners.FragmentBackPressedListener;
import tomdrever.timetable.data.Day;
import tomdrever.timetable.data.TimetableContainer;
import tomdrever.timetable.data.listeners.DataValueChangedListener;
import tomdrever.timetable.databinding.FragmentEditTimetableBinding;

import java.util.Collections;

public class EditTimetableFragment extends Fragment implements CardTouchedListener, DataValueChangedListener {
    private TimetableContainer timetableContainer;
    private boolean isNewTimetable;

    private EditingFinishedListener editingFinishedListener;
    private FragmentBackPressedListener fragmentBackPressedListener;
    private DayClickedListener dayClickedListener;

    private DaysRecyclerViewAdapter recyclerViewAdapter;
    private ItemTouchHelper itemTouchHelper;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        timetableContainer.getTimetable().setValueChangedListener(this);

        // Bind timetable to layout
        FragmentEditTimetableBinding binding = DataBindingUtil.inflate(inflater, R.layout.fragment_edit_timetable, container, false);
        binding.setTimetableContainer(timetableContainer);
        binding.setIsnewtimetable(isNewTimetable);

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //region Toolbar
        Toolbar toolbar = (Toolbar) getView().findViewById(R.id.edit_timetable_toolbar);
        setHasOptionsMenu(true);
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fragmentBackPressedListener.onFragmentBackPressed();
            }
        });
        //endregion

        //region RecyclerView
        RecyclerView daysListRecyclerView = (RecyclerView) getView().findViewById(R.id.edit_timetable_days_list_recyclerview);

        recyclerViewAdapter = new DaysRecyclerViewAdapter(timetableContainer.getTimetable().getDays(), true, this);

        daysListRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        daysListRecyclerView.setAdapter(recyclerViewAdapter);
        //endregion

        //region FAB
        FloatingActionButton fab = (FloatingActionButton) getView().findViewById(R.id.new_day_fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                timetableContainer.getTimetable().addDay(new Day(
                		String.format("Day %s", timetableContainer.getTimetable().getDays().size() + 1)));
            }
        });
        //endregion

        //region ItemTouchHelper
        ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

            @Override
            public boolean isItemViewSwipeEnabled() {
                return true;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {
                final Day tempDay = timetableContainer.getTimetable().getDays().get(viewHolder.getAdapterPosition());
                final int tempPosition = viewHolder.getAdapterPosition();
                timetableContainer.getTimetable().removeDay(tempPosition);
                Snackbar.make(viewHolder.itemView, tempDay.getName() + " deleted", Snackbar.LENGTH_SHORT).setAction("Undo", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Re-add
                        timetableContainer.getTimetable().addDay(tempDay, tempPosition);
                    }
                }).show();

            }

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                int fromPosition = viewHolder.getAdapterPosition();
                int toPosition = target.getAdapterPosition();

                if (fromPosition < toPosition) {
                    for (int i = fromPosition; i < toPosition; i++) {
                        Collections.swap(timetableContainer.getTimetable().getDays(), i, i + 1);
                    }
                } else {
                    for (int i = fromPosition; i > toPosition; i--) {
                        Collections.swap(timetableContainer.getTimetable().getDays(), i, i - 1);
                    }
                }

                recyclerViewAdapter.notifyItemMoved(fromPosition, toPosition);

                return true;
            }
        };
        //endregion

        itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        itemTouchHelper.attachToRecyclerView(daysListRecyclerView);

        // Set name and description to text boxes
        if (!isNewTimetable) {
            ((EditText) getView().findViewById(R.id.edit_timetable_name)).setText(timetableContainer.getName());
            EditText descriptionText = ((EditText) getView().findViewById(R.id.edit_timetable_description));
            if (!descriptionText.getText().toString().equals("No description")) {
                descriptionText.setText(timetableContainer.getDescription());
            }
        }
    }

    public static EditTimetableFragment newInstance(TimetableContainer timetableContainer, boolean isNewTimetable,
                                                    EditingFinishedListener editingFinishedListener,
                                                    FragmentBackPressedListener fragmentBackPressedListener,
                                                    DayClickedListener dayClickedListener) {
        EditTimetableFragment newFragment = new EditTimetableFragment();

        newFragment.isNewTimetable = isNewTimetable;
        newFragment.timetableContainer = timetableContainer;

        newFragment.editingFinishedListener = editingFinishedListener;
        newFragment.fragmentBackPressedListener = fragmentBackPressedListener;
        newFragment.dayClickedListener = dayClickedListener;

        return newFragment;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_edit, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_finish_editing:
                EditText editTimetableName = (EditText) getView().findViewById(R.id.edit_timetable_name);
                // Check the timetable has been given a name
                String name = editTimetableName.getText().toString().trim();
                if (name.isEmpty()) {
                    // If not, tell the user
                    Toast.makeText(getContext(), "The timetable needs a name!", Toast.LENGTH_SHORT).show();

                    // TODO - highlight name field to the user in some way - USER, DO THIS!
                } else {
                    // TODO - if (isnewtimetable) - check if the name is already used.
                    // If so, ask the user if they want to overwrite the tt or go back

                    TimetableFileManager fileManager = new TimetableFileManager(getContext());

                    // If it's not a new timetable, delete the old file (just in case)
                    if (!isNewTimetable) fileManager.delete(timetableContainer.getName());

                    // Set name and description from text
                    timetableContainer.setName(editTimetableName.getText().toString());

                    EditText editTimetableDescription = (EditText) getView().findViewById(R.id.edit_timetable_description);
                    String trimmedEditTextDescriptionString = editTimetableDescription.getText().toString().trim();
                    if (!trimmedEditTextDescriptionString.isEmpty()) {
                        timetableContainer.setDescription(editTimetableDescription.getText().toString());
                    }

                    // Save over timetable
                    fileManager.save(timetableContainer);

                    Toast.makeText(getContext(), "Saved " + timetableContainer.getName(), Toast.LENGTH_SHORT).show();

                    editingFinishedListener.onEditingTimetableFinished(timetableContainer);
                }

                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onValueAdded(int position) {
        recyclerViewAdapter.notifyItemInserted(position);
    }

    @Override
    public void onValueRemoved(int position) {
        recyclerViewAdapter.notifyItemRemoved(position);
    }

    @Override
    public void onCardClicked(RecyclerView.ViewHolder viewHolder, int position) {
        dayClickedListener.onDayClicked(position);
    }

    @Override
    public void onCardDragHandleTouched(RecyclerView.ViewHolder viewHolder, int position) {
        itemTouchHelper.startDrag(viewHolder);
    }

    public interface DayClickedListener {
        void onDayClicked(int position);
    }
}
