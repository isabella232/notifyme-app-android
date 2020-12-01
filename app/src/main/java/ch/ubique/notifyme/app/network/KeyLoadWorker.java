package ch.ubique.notifyme.app.network;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.crowdnotifier.android.sdk.CrowdNotifier;
import org.crowdnotifier.android.sdk.model.ExposureEvent;
import org.crowdnotifier.android.sdk.model.ProblematicEventInfo;

import ch.ubique.notifyme.app.utils.DiaryStorage;
import ch.ubique.notifyme.app.utils.NotificationHelper;

public class KeyLoadWorker extends Worker {

	public static final String NEW_NOTIFICATION = "NEW_NOTIFICATION";
	private static final String WORK_TAG = "ch.ubique.notifyme.app.network.KeyLoadWorker";
	private static final int DAYS_TO_KEEP_VENUE_VISITS = 14;
	private static final int REPEAT_INTERVAL_MINUTES = 120;
	private static final String LOG_TAG = "KeyLoadWorker";


	public static void startKeyLoadWorker(Context context) {
		Constraints constraints = new Constraints.Builder()
				.setRequiredNetworkType(NetworkType.CONNECTED)
				.build();

		PeriodicWorkRequest periodicWorkRequest =
				new PeriodicWorkRequest.Builder(KeyLoadWorker.class, REPEAT_INTERVAL_MINUTES, TimeUnit.MINUTES)
						.setConstraints(constraints)
						.build();

		WorkManager workManager = WorkManager.getInstance(context);
		workManager.enqueueUniquePeriodicWork(WORK_TAG, ExistingPeriodicWorkPolicy.KEEP, periodicWorkRequest);
	}


	public KeyLoadWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
		super(context, workerParams);
	}

	@NonNull
	@Override
	public Result doWork() {
		Log.d(LOG_TAG, "Started KeyLoadWorker");
		List<ProblematicEventInfo> problematicEventInfos = new TraceKeysServiceController(getApplicationContext()).loadTraceKeys();
		if (problematicEventInfos == null) {
			Log.d(LOG_TAG, "KeyLoadWorker failure");
			return Result.retry();
		}
		List<ExposureEvent> exposures = CrowdNotifier.checkForMatches(problematicEventInfos, getApplicationContext());
		if (!exposures.isEmpty()) {
			for (ExposureEvent exposureEvent : exposures) {
				new NotificationHelper(getApplicationContext()).showExposureNotification(exposureEvent.getId());
			}
			LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(new Intent(NEW_NOTIFICATION));
		}
		CrowdNotifier.cleanUpOldData(getApplicationContext(), DAYS_TO_KEEP_VENUE_VISITS);
		DiaryStorage.getInstance(getApplicationContext()).removeEntriesBefore(DAYS_TO_KEEP_VENUE_VISITS);

		Log.d(LOG_TAG, "KeyLoadWorker success");
		return Result.success();
	}

}
