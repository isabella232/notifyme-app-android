package ch.ubique.notifyme.app.diary;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import ch.ubique.notifyme.app.R;
import ch.ubique.notifyme.app.model.DiaryEntry;
import ch.ubique.notifyme.app.utils.DiaryStorage;

public class HideInDiaryDialogFragment extends DialogFragment {

	public final static String TAG = HideInDiaryDialogFragment.class.getCanonicalName();
	private final static String ARG_DIARY_ENTRY_ID = "ARG_DIARY_ENTRY_ID";

	private DiaryStorage diaryStorage;
	private DiaryEntry diaryEntry;

	public static HideInDiaryDialogFragment newInstance(long entryId) {
		HideInDiaryDialogFragment fragment = new HideInDiaryDialogFragment();
		Bundle args = new Bundle();
		args.putLong(ARG_DIARY_ENTRY_ID, entryId);
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		diaryStorage = DiaryStorage.getInstance(getContext());
		diaryEntry = diaryStorage.getDiaryEntryWithId(getArguments().getLong(ARG_DIARY_ENTRY_ID));
	}

	@Override
	public void onResume() {
		getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		getDialog().getWindow().setBackgroundDrawableResource(R.drawable.dialog_background);

		super.onResume();
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
			@Nullable Bundle savedInstanceState) {
		return inflater.inflate(R.layout.dialog_fragment_hide_in_diary, container);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		View closeButton = view.findViewById(R.id.remove_diary_entry_dialog_close_button);
		View hideButton = view.findViewById(R.id.hide_diary_entry_dialog_hide_button);
		TextView explanationText = view.findViewById(R.id.hide_diary_entry_dialog_text);

		String locationInfo = diaryEntry.getVenueInfo().getDescription() + ", " + diaryEntry.getVenueInfo().getAddress();
		explanationText.setText(getString(ch.ubique.notifyme.base.R.string.remove_diary_warning_text).replace("{LOCATION_INFO}", locationInfo));
		closeButton.setOnClickListener(v -> dismiss());
		hideButton.setOnClickListener(v -> hideNow());
	}

	private void hideNow() {
		diaryStorage.removeEntry(diaryEntry.getId());
		dismiss();
		requireActivity().getSupportFragmentManager().popBackStack();
	}

}
