package tomdrever.timetable.android.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.joda.time.LocalTime;

import java.util.regex.Pattern;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import tomdrever.timetable.R;
import tomdrever.timetable.data.Period;
import tomdrever.timetable.utils.TimeUtils;

public class EditPeriodDialogFragment extends DialogFragment {
	private Period period;

	private Unbinder unbinder;
	private View view;

	private int periodPosition;

    private FragmentManager fragmentManager;

	@BindView(R.id.period_name_input_layout) TextInputLayout periodNameInputLayout;
	@BindView(R.id.edit_period_name) EditText nameEditText;

    @BindView(R.id.edit_period_start_time_main_view) TextView startTimeMainTextView;
    @BindView(R.id.edit_period_start_time_second_view) TextView startTimeSecondTextView;
    @BindView(R.id.edit_period_end_time_main_view) TextView endTimeMainTextView;
    @BindView(R.id.edit_period_end_time_second_view) TextView endTimeSecondTextView;

    private PeriodDialogListener periodDialogListener;

	public static EditPeriodDialogFragment newInstance(Period period, int periodPosition,
													   PeriodDialogListener periodDialogListener,
                                                       FragmentManager fragmentManager) {
		EditPeriodDialogFragment fragment = new EditPeriodDialogFragment();
		fragment.period = period;
		fragment.periodPosition = periodPosition;
        fragment.periodDialogListener = periodDialogListener;
        fragment.fragmentManager = fragmentManager;
		return fragment;
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		if (period == null) period = new Period("New Period");

		unbinder = ButterKnife.bind(this, view);

		nameEditText.setText(period.getName() != null ? period.getName() : "");
		periodNameInputLayout.setHint(getActivity().getString(R.string.edit_timetable_name));

        if (period.getStartTime() == null) {
            period.setStartTime(new LocalTime(13, 15));
        }

        String[] startTime = TimeUtils.formatTime(period.getStartTime().getHourOfDay(), period.getStartTime().getMinuteOfHour()).split(Pattern.quote(" "));

        startTimeMainTextView.setText(startTime[0]);
        startTimeSecondTextView.setText(startTime[1]);

        if (period.getEndTime() == null) {
            period.setEndTime(new LocalTime(5, 30));
        }

        String[] endTime = TimeUtils.formatTime(period.getEndTime().getHourOfDay(), period.getEndTime().getMinuteOfHour()).split(Pattern.quote(" "));

        endTimeMainTextView.setText(endTime[0]);
        endTimeSecondTextView.setText(endTime[1]);

		return super.onCreateView(inflater, container, savedInstanceState);
	}

	@NonNull
	@Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
		// region Set Up Dialog
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		// Get the layout inflater
		LayoutInflater inflater = getActivity().getLayoutInflater();

		view = inflater.inflate(R.layout.edit_period_dialog, null);

		builder.setView(view);

		builder.setTitle(period == null ? "New Period" : String.format("Edit %s", period.getName()));

		builder.setPositiveButton("OK", null);
		if (period != null) builder.setNeutralButton("Delete", null);
		builder.setNegativeButton("Cancel", null);

		final AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);

		dialog.setOnShowListener(new DialogInterface.OnShowListener() {
			@Override
			public void onShow(DialogInterface dialogInterface) {
				// region On Positive Clicked
				Button posButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
				posButton.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						// Check the period has been given a name
						String name = nameEditText.getText().toString().trim();
						if (name.isEmpty()) {
							// If not, tell the user
							Toast.makeText(getActivity(), "The period needs a name!", Toast.LENGTH_SHORT).show();
						} else {
							period.setName(name);
							if (periodDialogListener != null)
								periodDialogListener.onFinishEditingPeriodClicked(period, periodPosition);

							dismiss();
						}
					}
				});
				// endregion

				// region On Neutral Clicked
				Button neutButton = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);

				// If there is not a neutral button, do not attempt to give it an onclick;
				if (neutButton == null) return;

				neutButton.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						periodDialogListener.onDeletePeriodClicked(periodPosition);
						dismiss();
					}
				});
				// endregion
			}
		});

        return dialog;
    }

	@OnClick(R.id.edit_period_start_time_view)
    void onStartTimeClicked() {
        final TimePickerFragmentDialog dialog = TimePickerFragmentDialog.newInstance(new TimePickerFragmentDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(int hour, int minute) {
                period.setStartTime(new LocalTime(hour, minute));

                String[] time = TimeUtils.formatTime(hour, minute).split(Pattern.quote(" "));

                startTimeMainTextView.setText(time[0]);
                startTimeSecondTextView.setText(time[1]);
            }
        }, period.getStartTime().getHourOfDay(), period.getStartTime().getMinuteOfHour());

        dialog.show(fragmentManager, "start_time");
    }

    @OnClick(R.id.edit_period_end_time_view)
    void onEndTimeClicked() {
        TimePickerFragmentDialog dialog = TimePickerFragmentDialog.newInstance(new TimePickerFragmentDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(int hour, int minute) {
                period.setEndTime(new LocalTime(hour, minute));

                String[] time = TimeUtils.formatTime(hour, minute).split(Pattern.quote(" "));

                endTimeMainTextView.setText(time[0]);
                endTimeSecondTextView.setText(time[1]);
            }
        }, period.getEndTime().getHourOfDay(), period.getEndTime().getMinuteOfHour());

        dialog.show(fragmentManager, "end_time");
    }

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		unbinder.unbind();
		unbinder = null;
	}

	public interface PeriodDialogListener {
		void onFinishEditingPeriodClicked(Period period, int periodPosition);
		void onDeletePeriodClicked(int periodPosition);
	}
}
