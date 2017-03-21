package works.idna.iddna.my_treatment_plan.utils.deserializer;

import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import works.idna.iddna.my_treatment_plan.entity.booster.Booster;
import works.idna.iddna.my_treatment_plan.entity.booster.BoosterDayWeek;
import works.idna.iddna.my_treatment_plan.entity.booster.BoosterNightWeek;
import works.idna.iddna.my_treatment_plan.entity.booster.BoosterWrapper;
import works.idna.iddna.my_treatment_plan.entity.TreatmentPlanConstants;


/**
 * Adaptation a json response to model
 *
 * @author      gvard
 * @since       1.0
 */
public class BoosterWrapperDeserializer implements JsonDeserializer<BoosterWrapper> {
    @Override
    public BoosterWrapper deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        BoosterWrapper boosterWrapper = new BoosterWrapper();
        JsonObject     object         = (JsonObject) json;
        Gson           gson           = new Gson();

        List<Integer> boosterIds      = gson.fromJson(object.get("id"),      new TypeToken<List<Integer>>(){}.getType());
        List<String>  boosterLabels   = gson.fromJson(object.get("label"),   new TypeToken<List<String>>(){}.getType());
        List<Integer> boosterQty      = gson.fromJson(object.get("qty"),     new TypeToken<List<Integer>>(){}.getType());
        List<String>  boosterBarCodes = gson.fromJson(object.get("barcode"), new TypeToken<List<String>>(){}.getType());
        JsonObject    dosagesObject   = object.get("dosages").getAsJsonObject();
        int boosterTotal              = object.get("booster_total").getAsInt();
        int boosterTotalQty           = object.get("booster_total_qty").getAsInt();
        boosterWrapper.boosterList    = createBoosterList(boosterIds, boosterLabels, boosterQty, boosterBarCodes, boosterTotal, boosterTotalQty);

        parseWeeks(boosterWrapper, dosagesObject);
        return boosterWrapper;
    }

    /**
     *
     * Parse json object and fill data to boosterWrapper.
     *
     * @param  boosterWrapper   BoosterWrapper object
     * @param  dosagesObject    Json object
     */
    private void parseWeeks(BoosterWrapper boosterWrapper, JsonObject dosagesObject) {

        String[] weekDays   = new String[]{TreatmentPlanConstants.WEEK1_DAYS, TreatmentPlanConstants.WEEK2_DAYS, TreatmentPlanConstants.WEEK3_DAYS,TreatmentPlanConstants.WEEK4_DAYS,TreatmentPlanConstants.WEEK5_DAYS,TreatmentPlanConstants.WEEK6_DAYS,TreatmentPlanConstants.WEEK7_DAYS,TreatmentPlanConstants.WEEK8_DAYS,TreatmentPlanConstants.WEEK9_DAYS,TreatmentPlanConstants.WEEK10_DAYS,TreatmentPlanConstants.WEEK11_DAYS,TreatmentPlanConstants.WEEK12_DAYS,TreatmentPlanConstants.WEEK13_DAYS,TreatmentPlanConstants.WEEK14_DAYS,TreatmentPlanConstants.WEEK15_DAYS };
        String[] weekDose   = new String[]{TreatmentPlanConstants.WEEK1_DOSE, TreatmentPlanConstants.WEEK2_DOSE, TreatmentPlanConstants.WEEK3_DOSE, TreatmentPlanConstants.WEEK4_DOSE, TreatmentPlanConstants.WEEK5_DOSE, TreatmentPlanConstants.WEEK6_DOSE, TreatmentPlanConstants.WEEK7_DOSE, TreatmentPlanConstants.WEEK8_DOSE, TreatmentPlanConstants.WEEK9_DOSE, TreatmentPlanConstants.WEEK10_DOSE, TreatmentPlanConstants.WEEK11_DOSE, TreatmentPlanConstants.WEEK12_DOSE, TreatmentPlanConstants.WEEK13_DOSE, TreatmentPlanConstants.WEEK14_DOSE, TreatmentPlanConstants.WEEK15_DOSE};
        String[] weekDosage = new String[]{TreatmentPlanConstants.WEEK1_DOSAGE, TreatmentPlanConstants.WEEK2_DOSAGE, TreatmentPlanConstants.WEEK3_DOSAGE, TreatmentPlanConstants.WEEK4_DOSAGE, TreatmentPlanConstants.WEEK5_DOSAGE, TreatmentPlanConstants.WEEK6_DOSAGE, TreatmentPlanConstants.WEEK7_DOSAGE, TreatmentPlanConstants.WEEK8_DOSAGE, TreatmentPlanConstants.WEEK9_DOSAGE, TreatmentPlanConstants.WEEK10_DOSAGE, TreatmentPlanConstants.WEEK11_DOSAGE, TreatmentPlanConstants.WEEK12_DOSAGE, TreatmentPlanConstants.WEEK13_DOSAGE, TreatmentPlanConstants.WEEK14_DOSAGE, TreatmentPlanConstants.WEEK15_DOSAGE};

        List<BoosterDayWeek>   dayWeekList   = new ArrayList<>();
        List<BoosterNightWeek> nightWeekList = new ArrayList<>();

        JsonObject day   = dosagesObject.getAsJsonObject("DAY");
        JsonObject night = dosagesObject.getAsJsonObject("NIGHT");

        String daysDay;
        String doseDay;
        String dosageDay;

        String daysNight;
        String doseNight;
        String dosageNight;

        BoosterDayWeek   weekDay;
        BoosterNightWeek weekNight;
        for (int position = 0, size = weekDays.length; position < size; position++) {
            weekDay   = new BoosterDayWeek();
            weekNight = new BoosterNightWeek();

            daysDay   = day.get(weekDays[position])   != null ? day.get(weekDays[position]).getAsString()   : null;
            doseDay   = day.get(weekDose[position])   != null ? day.get(weekDose[position]).getAsString()   : null;
            dosageDay = day.get(weekDosage[position]) != null ? day.get(weekDosage[position]).getAsString() : null;
            if(!TextUtils.isEmpty(daysDay) && !TextUtils.isEmpty(daysDay) && !TextUtils.isEmpty(daysDay)) {
                weekDay.weekDays   = daysDay;
                weekDay.weekDose   = doseDay;
                weekDay.weekDosage = dosageDay;
                weekDay.numOfWee   = position + 1;
                weekDay.type       = TreatmentPlanConstants.TYPE_BOOSTER_DAY;
                dayWeekList.add(weekDay);
            }

            daysNight   = night.get(weekDays[position])   != null ? night.get(weekDays[position]).getAsString()   : null;
            doseNight   = night.get(weekDose[position])   != null ? night.get(weekDose[position]).getAsString()   : null;
            dosageNight = night.get(weekDosage[position]) != null ? night.get(weekDosage[position]).getAsString() : null;
            if(!TextUtils.isEmpty(daysNight) && !TextUtils.isEmpty(doseNight) && !TextUtils.isEmpty(dosageNight)) {
                weekNight.weekDays   = daysNight;
                weekNight.weekDose   = doseNight;
                weekNight.weekDosage = dosageNight;
                weekNight.numOfWee   = position + 1;
                weekNight.type       = TreatmentPlanConstants.TYPE_BOOSTER_NIGHT;
                nightWeekList.add(weekNight);
            }
        }
        boosterWrapper.weekDayList   = dayWeekList;
        boosterWrapper.weekNightList = nightWeekList;
    }

    /**
     *
     * Creates a list of booster.
     *
     * @param  boosterIds       list of booster id
     * @param  boosterLabels    list of booster label
     * @param  boosterQty       list of booster quality
     * @param  boosterBarCodes  list of booster bar code
     * @param  boosterTotal     booster count
     * @param  boosterTotalQty  booster total quality
     * @return list of booster
     */
    private List<Booster> createBoosterList(List<Integer> boosterIds, List<String> boosterLabels,
                                            List<Integer> boosterQty, List<String> boosterBarCodes,
                                            int boosterTotal, int boosterTotalQty) {
        List<Booster> boosterList = new ArrayList<>(boosterTotalQty);
        Booster booster;
        int weekNumber = 0;
        for(int position = 0; position < boosterTotal; position++) {
            booster           = new Booster();
            booster.boosterId = boosterIds.get(position);
            booster.barcode   = boosterBarCodes.get(position);
            booster.label     = boosterLabels.get(position);
            for(int count = 0, size = boosterQty.get(position); count < size; count++) {
                weekNumber++;
                if(weekNumber != 1)
                    try {
                        booster = (Booster) booster.clone();
                    } catch (CloneNotSupportedException e) {
                        Log.e(BoosterWrapperDeserializer.class.getSimpleName(), e.toString());
                    }
                booster.weekNum = weekNumber;
                boosterList.add(booster);
            }
        }
        return boosterList;
    }
}