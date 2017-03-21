package works.idna.iddna.my_treatment_plan.adapter;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.support.v7.widget.RecyclerView;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import works.idna.iddna.R;
import works.idna.iddna.calendar.HourPart;
import works.idna.iddna.calendar.MonthCircleView;
import works.idna.iddna.db.repository.nutricosmetic.NutricosmeticDayRepository;
import works.idna.iddna.my_treatment_plan.contract.TreatmentDayContract;
import works.idna.iddna.my_treatment_plan.entity.TreatmentDay;
import works.idna.iddna.my_treatment_plan.entity.nutricosmetic.NutricosmeticDay;
import works.idna.iddna.my_treatment_plan.interfaces.OnWeekClickListener;
import works.idna.iddna.utils.BitmapUtils;
import works.idna.iddna.utils.DateTimeUtils;
import works.idna.iddna.utils.HourParserUtils;
import works.idna.iddna.utils.UserPreferences;

public class MonthTreatmentAdapter extends RecyclerView.Adapter<MonthTreatmentAdapter.TreatmentHolder>{

    private List<TreatmentDay>   mTreatmentDayList;
    private List<List<HourPart>> mFoodTimeLists;

    private Bitmap mFoundationBitmap;
    private Bitmap mNutricosmeticBitmap;

    private int mDrawableFoundation;
    private int mDrawableNutricosmetic;

    private WeakReference<Context> mContextReference;

    private OnWeekClickListener mOnWeekClickListener;

    @SuppressWarnings("deprecation")
    public MonthTreatmentAdapter(Context context) {
        Resources res = context.getResources();

        mFoundationBitmap    = BitmapUtils.drawableToBitmap(res.getDrawable(R.mipmap.gel_icon_white));
        mNutricosmeticBitmap = BitmapUtils.drawableToBitmap(res.getDrawable(R.mipmap.nutr));

        mDrawableFoundation    = R.mipmap.gel_icon_white;
        mDrawableNutricosmetic = R.mipmap.nutr;

        mContextReference = new WeakReference<>(context);

        if(context instanceof OnWeekClickListener)
            mOnWeekClickListener = (OnWeekClickListener) context;
    }

    @Override
    public TreatmentHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_month, parent, false);
        return new TreatmentHolder(view);
    }

    @Override
    public int getItemCount() {
        return mTreatmentDayList != null ? mTreatmentDayList.size() : 0;
    }

    @Override
    public void onBindViewHolder(TreatmentHolder holder, int position) {
        TreatmentDay treatmentDay = mTreatmentDayList.get(position);
        holder.setOnClickListener(treatmentDay, mOnWeekClickListener);
        holder.mCircleView.clearHourParts();
        if(treatmentDay != null) {
            holder.mCircleView.setShowText(true, DateTimeUtils.getDdFormat().format(DateTimeUtils.createDateFromString(treatmentDay.timestamp)));

            if (UserPreferences.getCurrentProgram() == UserPreferences.DNA_DIET)
                showDiet(position, holder);

            if(treatmentDay.weekNum == TreatmentDayContract.NO_SUCH_DAY_IN_THREATMENT_PLAN)return;

            if (UserPreferences.getCurrentProgram() == UserPreferences.DNA_AGE)
                showAge(treatmentDay, holder);
        }
    }

    private void showAge(TreatmentDay treatmentDay, TreatmentHolder holder) {
        showFoundation(treatmentDay, holder);
        showNutricosmetic(treatmentDay, holder);
    }

    private void showDiet(int position, TreatmentHolder holder) {
        if(mFoodTimeLists != null && mFoodTimeLists.size() > position)
            holder.mCircleView.addPeriods(mFoodTimeLists.get(position));
    }

    public void setOnWeekClickListener(OnWeekClickListener weekClickListener) {
        mOnWeekClickListener = weekClickListener;
    }

    public void addFoodTimeList(List<List<HourPart>> list) {
        mFoodTimeLists = list;
    }

    private void showFoundation(TreatmentDay treatmentDay, TreatmentHolder holder) {
        Pair<Integer, Integer> planDay    = HourParserUtils.getHours(treatmentDay.treatmentPlanDayTime);
        Pair<Integer, Integer> nightDay   = HourParserUtils.getHours(treatmentDay.treatmentPlanNightTime);

        if (treatmentDay.isFoundationDay)
            holder.mCircleView.addPartForPeriod(planDay.first, planDay.second, mFoundationBitmap, mDrawableFoundation);
        if (treatmentDay.isFoundationNight)
            holder.mCircleView.addPartForPeriod(nightDay.first, nightDay.second, mFoundationBitmap, mDrawableFoundation);
    }

    private void showNutricosmetic(TreatmentDay treatmentDay, TreatmentHolder holder) {
        if(mContextReference.get() == null) return;
        NutricosmeticDay nutricosmeticDay = NutricosmeticDayRepository.getNutricosmeticDay(mContextReference.get() , treatmentDay.timestamp);
        if(nutricosmeticDay != null) {
            Pair<Integer, Integer> planDay  = HourParserUtils.getHours(nutricosmeticDay.nutricosmeticPlanDayTime);
            Pair<Integer, Integer> noonDay  = HourParserUtils.getHours(nutricosmeticDay.nutricosmeticPlanNoonTime);
            Pair<Integer, Integer> nightDay = HourParserUtils.getHours(nutricosmeticDay.nutricosmeticPlanNightTime);
            holder.mCircleView.addPartForPeriod(planDay.first,  planDay.second,  mNutricosmeticBitmap, mDrawableNutricosmetic);
            holder.mCircleView.addPartForPeriod(noonDay.first,  noonDay.second,  mNutricosmeticBitmap, mDrawableNutricosmetic);
            holder.mCircleView.addPartForPeriod(nightDay.first, nightDay.second, mNutricosmeticBitmap, mDrawableNutricosmetic);
        }
    }

    public void addTreatmentDays(List<TreatmentDay> treatmentDayList) {
        if(mTreatmentDayList != null)
            mTreatmentDayList.clear();
        mTreatmentDayList = treatmentDayList;
        notifyDataSetChanged();
    }

    static class TreatmentHolder extends RecyclerView.ViewHolder {
        private MonthCircleView mCircleView;
        TreatmentHolder(View itemView) {
            super(itemView);
            mCircleView = (MonthCircleView) itemView.findViewById(R.id.circle_view);
        }

        void setOnClickListener(final TreatmentDay treatmentDay,final OnWeekClickListener onWeekClickListener) {
            mCircleView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onWeekClickListener.onWeekClick(treatmentDay);
                }
            });
        }
    }
}