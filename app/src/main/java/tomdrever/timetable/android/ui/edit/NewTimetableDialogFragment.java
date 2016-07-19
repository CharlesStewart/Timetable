package tomdrever.timetable.android.ui.edit;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.util.Arrays;
import java.util.Calendar;

import tomdrever.timetable.R;
import tomdrever.timetable.android.TimetableFileManager;
import tomdrever.timetable.android.ui.view.ViewTimetableActivity;
import tomdrever.timetable.data.Timetable;
import tomdrever.timetable.data.TimetableContainer;

public class NewTimetableDialogFragment extends DialogFragment{

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        final View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_new_timetable, null);

        builder.setView(view)
            .setPositiveButton(R.string.dialog_new_timetable_create, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    EditText editTextName = (EditText) view.findViewById(R.id.edit_timetable_name);
                    EditText editTextDescription = (EditText) view.findViewById(R.id.edit_timetable_description);

                    if (editTextName.getText().toString().equals("")) {
                        Toast.makeText(getActivity(), "Name field required", Toast.LENGTH_SHORT).show();
                    }
                    else if (Arrays.asList(new File(new TimetableFileManager(getContext()).directory).list()).contains(editTextName.getText().toString())) {
                        Toast.makeText(getActivity(), "Timetable with that name already exists", Toast.LENGTH_SHORT).show();
                    } else {
                        // create new timetable - launch view timetable, telling it to immediately run edit timetable
                        Intent intent = new Intent(getActivity(), ViewTimetableActivity.class);
                        intent.putExtra("isnewtimetable", true);

                        // Send new empty timetable with name and description
                        intent.putExtra("timetabledetails", new TimetableContainer(
                                editTextName.getText().toString(),
                                editTextDescription.getText().toString(),
                                Calendar.getInstance().getTime(), new Timetable()));
                        getActivity().startActivityForResult(intent, 100);
                    }
                }
            })
            .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    NewTimetableDialogFragment.this.getDialog().cancel();
                }
            });

        return builder.create();
    }
}
