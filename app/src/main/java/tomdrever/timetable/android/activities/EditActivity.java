package tomdrever.timetable.android.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import tomdrever.timetable.R;
import tomdrever.timetable.android.fragments.EditDayFragment;
import tomdrever.timetable.android.fragments.EditTimetableFragment;
import tomdrever.timetable.android.listeners.EditingFinishedListener;
import tomdrever.timetable.android.listeners.FragmentBackPressedListener;
import tomdrever.timetable.data.Day;
import tomdrever.timetable.data.TimetableContainer;

public class EditActivity extends AppCompatActivity implements EditingFinishedListener,
        FragmentBackPressedListener, EditTimetableFragment.DayClickedListener {

    private TimetableContainer timetableContainer;

    private boolean isNewTimetable;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_template);

        Intent intent = getIntent();

        isNewTimetable = intent.getBooleanExtra("isnewtimetable", false);

        timetableContainer = new TimetableContainer((TimetableContainer) intent.getSerializableExtra("timetable"));

        transitionTo(EditTimetableFragment.newInstance(timetableContainer, isNewTimetable, this, this, this), false);
    }

    private void transitionTo(Fragment fragment, boolean addToBackStack) {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.fragment_placeholder, fragment);
        if (addToBackStack) fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    @Override
    public void onBackPressed() {
        onFragmentBackPressed();
    }

    @Override
    public void onFragmentBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
	        FragmentManager fm = getSupportFragmentManager();
            fm.popBackStack();
        } else {
            TimetableContainer initialTimetableContainer = (TimetableContainer) getIntent().getSerializableExtra("timetable");

            if (!initialTimetableContainer.equals(timetableContainer)) {
                new AlertDialog.Builder(this)
                        .setTitle("Back")
                        .setMessage("Are you sure you want to discard your changes?")
                        .setPositiveButton("Discard", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                back();
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        })
                        .show();
            } else {
                back();
            }
        }
    }

    private void back() {
        Intent intent;

        if (isNewTimetable) {
            intent = new Intent(this, OverviewActivity.class);
        } else {
            intent = new Intent(this, ViewActivity.class);
            intent.putExtra("timetable", getIntent().getSerializableExtra("timetable"));
        }

        startActivity(intent);
        finish();
    }

    @Override
    public void onDayClicked(int position) {
        transitionTo(EditDayFragment.newInstance(new Day(timetableContainer.getTimetable().getDays().get(position)),
                position, this, this), true);
    }

    @Override
    public void onEditingTimetableFinished(TimetableContainer timetableContainer) {
        Intent intent = new Intent(this, ViewActivity.class);
        intent.putExtra("timetable", timetableContainer);
        startActivity(intent);
        finish();
    }

    @Override
    public void onEditingDayFinished(Day day, int position) {
	    timetableContainer.getTimetable().getDays().set(position, day);
	    // Remove EditDay from the backstack
	    getSupportFragmentManager().popBackStackImmediate();
	    //transitionTo(EditTimetableFragment.newInstance(timetableContainer, isNewTimetable, this, this, this), false);
    }
}