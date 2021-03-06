package tomdrever.timetable.android.controllers;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bluelinelabs.conductor.RouterTransaction;
import com.bluelinelabs.conductor.changehandler.FadeChangeHandler;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import tomdrever.timetable.R;
import tomdrever.timetable.android.controllers.base.BaseController;
import tomdrever.timetable.data.Timetable;

public class TimetableListController extends BaseController {

    private ArrayList<Timetable> timetables;

    private TimetableListAdapter adapter;

    @BindView(R.id.timetables_list_recyclerview) RecyclerView recyclerView;
    @BindView(R.id.no_timetables) TextView noTimetablesTextView;

    @Override
    protected View inflateView(@NonNull LayoutInflater inflater, @NonNull ViewGroup container) {
        return inflater.inflate(R.layout.controller_timetable_list, container, false);
    }

    @Override
    protected void onViewBound(@NonNull View view) {
        super.onViewBound(view);

        timetables = getFileManager().loadAll();

        updateNoTimetablesTextView();

        adapter = new TimetableListAdapter(LayoutInflater.from(view.getContext()));

        recyclerView.setLayoutManager(new LinearLayoutManager(view.getContext()));
        recyclerView.setAdapter(adapter);

        // region Swiping
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

            @Override
            public boolean isItemViewSwipeEnabled() { return true; }

            @Override
            public boolean isLongPressDragEnabled() { return false; }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {
                // Store timetable data temporarily, so it can be restored
                final Timetable tempTimetable = timetables.get(viewHolder.getAdapterPosition());
                final int tempPosition = viewHolder.getAdapterPosition();
                removeTimetableAt(tempPosition);
                Snackbar.make(viewHolder.itemView, tempTimetable.getName() + " deleted", Snackbar.LENGTH_SHORT).setAction("Undo", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Restore
                        addTimetableAt(tempTimetable, tempPosition);
                    }
                }).show();
            }

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                int fromPosition = viewHolder.getAdapterPosition();
                int toPosition = target.getAdapterPosition();

                if (fromPosition < toPosition) {
                    for (int i = fromPosition; i < toPosition; i++) {
                        swapTimetablesAt(i, i + 1);
                    }
                } else {
                    for (int i = fromPosition; i > toPosition; i--) {
                        swapTimetablesAt(i, i - 1);
                    }
                }

                adapter.notifyItemMoved(fromPosition, toPosition);

                return true;
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
        // endregion
    }

    private void addTimetableAt(Timetable timetable, int position) {
        getFileManager().save(timetable);
        timetables.add(position, timetable);
        adapter.notifyItemInserted(position);

        updateNoTimetablesTextView();
    }

    private void removeTimetableAt(int position) {
        getFileManager().delete(timetables.get(position).getId());
        timetables.remove(position);
        adapter.notifyItemRemoved(position);

        updateNoTimetablesTextView();
    }

    private void swapTimetablesAt(int position1, int position2) {
        Timetable timetable1 = timetables.get(position1);
        timetable1.setIndex(position2);
        getFileManager().save(timetable1);

        Timetable timetable2 = timetables.get(position2);
        timetable2.setIndex(position1);
        getFileManager().save(timetable2);

        timetables.set(position1, timetable2);
        timetables.set(position2, timetable1);
    }

    private void updateNoTimetablesTextView() {
        if (timetables.isEmpty())
            noTimetablesTextView.setVisibility(View.VISIBLE);
        else
            noTimetablesTextView.setVisibility(View.INVISIBLE);
    }

    @Override
    protected String getTitle() {
        return "Timetables";
    }

    @Override
    public boolean handleBack() {
        getActivity().finish();

        return true;
    }

    @Override
    protected void onSave(Bundle outState) {
        outState.putParcelableArrayList("timetables", timetables);
    }

    @Override
    protected void onRestore(Bundle inState) {
        timetables = inState.getParcelableArrayList("timetables");
    }

    @OnClick(R.id.new_timetable_fab)
    void onFabClick() {
        getRouter().pushController(RouterTransaction.with(new EditTimetableController(timetables.size()))
                .popChangeHandler(new FadeChangeHandler())
                .pushChangeHandler(new FadeChangeHandler()));
    }

    public class TimetableListAdapter extends RecyclerView.Adapter<TimetableListAdapter.TimetableViewHolder> {
        private final LayoutInflater inflater;

        public TimetableListAdapter(LayoutInflater inflater) {
            this.inflater = inflater;
        }

        @Override
        public TimetableViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new TimetableViewHolder(inflater.inflate(R.layout.view_card_view_timetable, parent, false));
        }

        @Override
        public void onBindViewHolder(TimetableViewHolder holder, int position) {
            holder.bind(timetables.get(position));
        }

        @Override
        public int getItemCount() {
            return timetables.size();
        }

        public class TimetableViewHolder extends RecyclerView.ViewHolder {

            @BindView(R.id.timetable_card_name) TextView nameTextView;
            @BindView(R.id.timetable_card_description) TextView descriptionTextView;

            Timetable timetable;

            public TimetableViewHolder(View itemView) {
                super(itemView);

                ButterKnife.bind(this, itemView);
            }

            public void bind(Timetable item) {
                this.timetable = item;

                nameTextView.setText(timetable.getName());
                descriptionTextView.setText(timetable.getDescription());

                if (timetable.getDescription().trim().isEmpty()) {
                    // TODO - change timetable card height?
                    descriptionTextView.setVisibility(View.GONE);
                } else {
                    descriptionTextView.setVisibility(View.VISIBLE);
                }
            }

            @OnClick(R.id.timetable_card_base_view)
            void onCardClicked() {
                getRouter().pushController(RouterTransaction.with(new ViewTimetableController(timetable))
                        .popChangeHandler(new FadeChangeHandler())
                        .pushChangeHandler(new FadeChangeHandler()));
            }
        }
    }
}
