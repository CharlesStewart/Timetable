package tomdrever.timetable.android.controllers;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.Toast;

import com.bluelinelabs.conductor.RouterTransaction;
import com.bluelinelabs.conductor.changehandler.FadeChangeHandler;

import java.lang.reflect.Field;

import butterknife.BindView;
import tomdrever.timetable.R;
import tomdrever.timetable.android.controllers.base.BaseController;
import tomdrever.timetable.android.fragments.DayFragment;
import tomdrever.timetable.data.Timetable;
import tomdrever.timetable.utils.FixedSpeedScroller;
import tomdrever.timetable.utils.ViewUtils;

public class ViewTimetableController extends BaseController {

    private int initialPosition;
    private Timetable timetable;

    @BindView(R.id.days_pager) ViewPager daysViewPager;
    @BindView(R.id.days_tablayout) TabLayout periodsTabLayout;

    private DaysPagerAdapter daysAdapter;

    public ViewTimetableController() {
        initialPosition = 0;
    }

    public ViewTimetableController(Timetable timetable) {
        this.timetable = timetable;
        initialPosition = 0;
    }
    public ViewTimetableController(Timetable timetable, int position) {
        this.timetable = timetable;
        initialPosition = position;
    }

    @Override
    protected View inflateView(@NonNull LayoutInflater inflater, @NonNull ViewGroup container) {
        return inflater.inflate(R.layout.controller_view_timetable, container, false);
    }

    @Override
    protected void onViewBound(@NonNull View view) {
        super.onViewBound(view);

        setRetainViewMode(RetainViewMode.RELEASE_DETACH);

        setHasOptionsMenu(true);

        daysAdapter = new DaysPagerAdapter(getAppCombatActivity().getSupportFragmentManager());
        daysViewPager.setAdapter(daysAdapter);

        // region Cap Pager Scrolling
        try {
            Field scrollerField = ViewPager.class.getDeclaredField("mScroller");
            scrollerField.setAccessible(true);
            FixedSpeedScroller scroller = new FixedSpeedScroller(getApplicationContext(), new DecelerateInterpolator());
            scrollerField.set(daysViewPager, scroller);
        } catch (NoSuchFieldException | IllegalAccessException | IllegalArgumentException ignored) {}
        // endregion

        periodsTabLayout.setupWithViewPager(daysViewPager);

        // region Set Custom Tabs
        LayoutInflater inflater = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        for (int i = 0; i < periodsTabLayout.getTabCount(); i++) {
            String text = String.valueOf(timetable.getDays().get(i).getName().toUpperCase().charAt(0));
            int colour = timetable.getDays().get(i).getColour();

            periodsTabLayout.getTabAt(i).setCustomView(ViewUtils.createCircleView(inflater, text, colour));
            periodsTabLayout.getTabAt(i).getCustomView().findViewById(R.id.circle_item_image).setPadding(20, 20, 20, 20);
        }
        // endregion

        // region Enlarge tab when selected
        if (periodsTabLayout.getTabCount() > 0 && initialPosition < periodsTabLayout.getTabCount()) {
            daysViewPager.setCurrentItem(initialPosition);

            TabLayout.Tab initialTab = periodsTabLayout.getTabAt(initialPosition);
            ViewUtils.startAnimation(initialTab.getCustomView(), R.anim.scale_day_up, getApplicationContext());
        }

        periodsTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                ViewUtils.startAnimation(tab.getCustomView(), R.anim.scale_day_up, getApplicationContext());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                ViewUtils.startAnimation(tab.getCustomView(), R.anim.scale_day_down, getApplicationContext());
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) { }
        });
        // endregion
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_view_timetable, menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_edit_view) {
            getRouter().pushController(RouterTransaction.with(new EditTimetableController(timetable))
                    .popChangeHandler(new FadeChangeHandler())
                    .pushChangeHandler(new FadeChangeHandler()));
        } else if (item.getItemId() == R.id.action_mode_switch) {
            Toast.makeText(getApplicationContext(), "Switch to context mode or to overview mode", Toast.LENGTH_SHORT).show();
        }

        return true;
    }

    @Override
    protected String getTitle() {
        return timetable.getName();
    }

    @Override
    protected String getSubtitle() {
        return timetable.getDescription();
    }

    @Override
    public boolean handleBack() {
        getRouter().pushController(RouterTransaction.with(new TimetableListController())
                .popChangeHandler(new FadeChangeHandler())
                .pushChangeHandler(new FadeChangeHandler()));

        return true;
    }

    @Override
    protected void onSave(Bundle outState) {
        if (daysViewPager != null) outState.putInt("position", daysViewPager.getCurrentItem());
        outState.putParcelable("timetable", timetable);
    }

    @Override
    protected void onRestore(Bundle inState) {
        initialPosition = inState.getInt("position");
        timetable = inState.getParcelable("timetable");
    }

    @Override
    protected boolean showUpNavigation() { return true; }

    private class DaysPagerAdapter extends FragmentStatePagerAdapter {

        public DaysPagerAdapter(FragmentManager manager) { super(manager); }

        @Override
        public Fragment getItem(int position) {
            return DayFragment.newDayFragment(timetable.getDays().get(position));
        }

        @Override
        public int getCount() { return timetable.getDays().size(); }

        @Override
        public CharSequence getPageTitle(int position) {
            return timetable.getDays().get(position).getName();
        }
    }
}
