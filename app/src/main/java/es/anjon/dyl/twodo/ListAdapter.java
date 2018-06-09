package es.anjon.dyl.twodo;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

import es.anjon.dyl.twodo.models.ListItem;

public class ListAdapter extends RecyclerView.Adapter<ListAdapter.ViewHolder> {

    private List<ListItem> mListItems;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView mTitleView;
        public TextView mPriorityView;
        public CheckBox mCheckedView;
        public TextView mDueDateView;
        public TextView mDueMonthView;
        public LinearLayout mCalendarView;
        public ViewHolder(View v) {
            super(v);
            mTitleView = v.findViewById(R.id.list_item_title);
            mPriorityView = v.findViewById(R.id.list_item_priority);
            mCheckedView = v.findViewById(R.id.list_item_checkbox);
            mDueDateView = v.findViewById(R.id.list_item_due_date);
            mDueMonthView = v.findViewById(R.id.list_item_due_month);
            mCalendarView = v.findViewById(R.id.list_item_calendar);
        }
    }

    public ListAdapter(List<ListItem> myDataset) {
        mListItems = myDataset;
    }

    @Override
    public ListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LinearLayout v = (LinearLayout) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        ListItem item = mListItems.get(position);
        holder.mTitleView.setText(item.getTitle());
        holder.mPriorityView.setBackgroundColor(item.getPrioirtyColour());
        holder.mCheckedView.setChecked(item.getChecked());
        if (item.getDueDate() == null) {
            holder.mCalendarView.setVisibility(View.GONE);
        } else {
            holder.mDueDateView.setText(item.getShortDueDate());
            holder.mDueMonthView.setText(item.getShortDueMonth());
            holder.mCalendarView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return mListItems.size();
    }

}
