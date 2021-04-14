package ch.ubique.notifyme.onboarding;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import ch.ubique.notifyme.base.utils.FeatureUtil;
import ch.ubique.notifyme.base.utils.Storage;

public class OnboardingActivity extends AppCompatActivity {

	private String url;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_onboarding);

		if (getIntent().getData() != null) {
			url = getIntent().getData().toString();
		}

		boolean onboardingCompleted = Storage.getInstance(this).getOnboardingCompleted();
		if (onboardingCompleted) {
			showMainActivity();
		} else {
			showOnboarding();
		}
	}

	private void showMainActivity() {
		Intent intent = FeatureUtil.createIntentForMain(this, url);
		startActivity(intent);
		finish();
	}

	private void showOnboarding() {
		getSupportFragmentManager().beginTransaction()
				.replace(R.id.container, OnboardingIntroductionFragment.newInstance())
				.commit();
	}

}
