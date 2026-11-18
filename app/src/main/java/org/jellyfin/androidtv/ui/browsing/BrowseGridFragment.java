package org.jellyfin.androidtv.ui.browsing;

import static org.koin.java.KoinJavaComponent.inject;

import android.os.Bundle;
import android.os.Handler;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.leanback.widget.BaseGridView;
import androidx.leanback.widget.FocusHighlight;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.OnItemViewSelectedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;
import androidx.leanback.widget.VerticalGridPresenter;
import androidx.lifecycle.Lifecycle;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.constant.ChangeTriggerType;
import org.jellyfin.androidtv.constant.CustomMessage;
import org.jellyfin.androidtv.constant.Extras;
import org.jellyfin.androidtv.constant.GridDirection;
import org.jellyfin.androidtv.constant.ImageType;
import org.jellyfin.androidtv.constant.PosterSize;
import org.jellyfin.androidtv.constant.QueryType;
import org.jellyfin.androidtv.data.model.FilterOptions;
import org.jellyfin.androidtv.data.querying.GetUserViewsRequest;
import org.jellyfin.androidtv.data.repository.CustomMessageRepository;
import org.jellyfin.androidtv.data.service.BackgroundService;
import org.jellyfin.androidtv.databinding.HorizontalGridBrowseBinding;
import org.jellyfin.androidtv.databinding.PopupEmptyBinding;
import org.jellyfin.androidtv.preference.LibraryPreferences;
import org.jellyfin.androidtv.preference.PreferencesRepository;
import org.jellyfin.androidtv.ui.AlphaPickerView;
import org.jellyfin.androidtv.ui.itemhandling.BaseRowItem;
import org.jellyfin.androidtv.ui.itemhandling.ItemLauncher;
import org.jellyfin.androidtv.ui.itemhandling.ItemRowAdapter;
import org.jellyfin.androidtv.ui.itemhandling.ItemRowAdapterHelperKt;
import org.jellyfin.androidtv.ui.presentation.CardPresenter;
import org.jellyfin.androidtv.ui.presentation.HorizontalGridPresenter;
import org.jellyfin.androidtv.ui.shared.toolbar.CatalogBarFocus;
import org.jellyfin.androidtv.ui.shared.toolbar.ContentPrimaryAction;
import org.jellyfin.androidtv.util.CoroutineUtils;
import org.jellyfin.androidtv.util.ImageHelper;
import org.jellyfin.androidtv.util.InfoLayoutHelper;
import org.jellyfin.androidtv.util.KeyProcessor;
import org.jellyfin.androidtv.util.Utils;
import org.jellyfin.androidtv.util.apiclient.EmptyResponse;
import org.jellyfin.sdk.api.client.ApiClient;
import org.jellyfin.sdk.model.api.BaseItemDto;
import org.jellyfin.sdk.model.api.BaseItemKind;
import org.jellyfin.sdk.model.api.CollectionType;
import org.jellyfin.sdk.model.api.ItemSortBy;
import org.jellyfin.sdk.model.api.SortOrder;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import kotlin.Lazy;
import kotlinx.serialization.json.Json;
import timber.log.Timber;

public class BrowseGridFragment extends Fragment implements View.OnKeyListener {
    private final static int CHUNK_SIZE_MINIMUM = 25;

    private String mainTitle;
    private FragmentActivity mActivity;
    private BaseRowItem mCurrentItem;
    private CompositeClickedListener mClickedListener = new CompositeClickedListener();
    private CompositeSelectedListener mSelectedListener = new CompositeSelectedListener();
    private final Handler mHandler = new Handler();
    private int mCardHeight;
    private BrowseRowDef mRowDef;
    private CardPresenter mCardPresenter;

    private boolean justLoaded = true;
    private boolean gridInitialized = false;
    // KFIN: survives view recreation when navigating into an item and back
    private int savedGridPosition = -1;
    private String mLastLoadSignature = null;
    private PosterSize mPosterSizeSetting = PosterSize.MED;
    private ImageType mImageType = ImageType.POSTER;
    private GridDirection mGridDirection = GridDirection.HORIZONTAL;
    private boolean determiningPosterSize = false;

    private UUID mParentId;
    private BaseItemDto mFolder;
    private LibraryPreferences libraryPreferences;

    private HorizontalGridBrowseBinding binding;
    private ItemRowAdapter mAdapter;
    private Presenter mGridPresenter;
    private Presenter.ViewHolder mGridViewHolder;
    private BaseGridView mGridView;
    private int mSelectedPosition = -1;
    private int mGridHeight = -1;
    private int mGridWidth = -1;
    private int mGridItemSpacingHorizontal = 0;
    private int mGridItemSpacingVertical = 0;
    private int mGridPaddingLeft = 0;
    private int mGridPaddingTop = 0;

    private final Lazy<BackgroundService> backgroundService = inject(BackgroundService.class);
    private final Lazy<PreferencesRepository> preferencesRepository = inject(PreferencesRepository.class);
    private final Lazy<CustomMessageRepository> customMessageRepository = inject(CustomMessageRepository.class);
    private final Lazy<ItemLauncher> itemLauncher = inject(ItemLauncher.class);
    private final Lazy<KeyProcessor> keyProcessor = inject(KeyProcessor.class);
    private final Lazy<ApiClient> api = inject(ApiClient.class);
    private final Lazy<ImageHelper> imageHelper = inject(ImageHelper.class);

    private int mCardsScreenEst = 0;
    private int mCardsScreenStride = 0;
    private double mCardFocusScale = 1.15; // 115%, just a default we use the resource card_scale_focus otherwise
    private final int MIN_NUM_CARDS = 5; // minimum number of visible cards we allow, this results in more empty space
    private final double CARD_SPACING_PCT = 1.0; // 100% expressed as relative to the padding_left/top, which depends on the mCardFocusScale and AspectRatio
    private final double CARD_SPACING_HORIZONTAL_BANNER_PCT = 0.5; // 50% allow horizontal card overlapping for banners, otherwise spacing is too large
    private final int VIEW_SELECT_UPDATE_DELAY = 250; // delay in ms until we update the top-row info for a selected item

    private boolean mDirty = true; // CardHeight, RowDef or GridSize changed

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // init with some working defaults
        DisplayMetrics display = requireContext().getResources().getDisplayMetrics();
        // grid starts at 55% of the screen height, so it occupies the bottom 45%
        mGridHeight = Math.round(display.heightPixels * 0.45f / getResources().getDisplayMetrics().density - 16f);
        mGridWidth = Math.round(display.widthPixels / getResources().getDisplayMetrics().density);


        mActivity = getActivity();

        mFolder = Json.Default.decodeFromString(BaseItemDto.Companion.serializer(), getArguments().getString(Extras.Folder));
        mParentId = mFolder.getId();
        mainTitle = mFolder.getName();
        libraryPreferences = preferencesRepository.getValue().getLibraryPreferences(Objects.requireNonNull(mFolder.getDisplayPreferencesId()));
        mPosterSizeSetting = libraryPreferences.get(LibraryPreferences.Companion.getPosterSize());
        mImageType = libraryPreferences.get(LibraryPreferences.Companion.getImageType());
        mGridDirection = libraryPreferences.get(LibraryPreferences.Companion.getGridDirection());
        mCardFocusScale = getResources().getFraction(R.fraction.card_scale_focus, 1, 1);

        if (mGridDirection.equals(GridDirection.VERTICAL))
            setGridPresenter(new VerticalGridPresenter(FocusHighlight.ZOOM_FACTOR_LARGE, false));
        else
            setGridPresenter(new HorizontalGridPresenter());

        sortOptions = new HashMap<>();
        {
            boolean isTvShows = mFolder.getCollectionType() == CollectionType.TVSHOWS;

            sortOptions.put(0, new SortOption(getString(R.string.lbl_name), ItemSortBy.SORT_NAME, SortOrder.ASCENDING));
            if (isTvShows) {
                sortOptions.put(1, new SortOption(getString(R.string.lbl_date_show_added), ItemSortBy.DATE_CREATED, SortOrder.DESCENDING));
            } else {
                sortOptions.put(1, new SortOption(getString(R.string.lbl_date_added), ItemSortBy.DATE_CREATED, SortOrder.DESCENDING));
            }
            sortOptions.put(2, new SortOption(getString(R.string.lbl_premier_date), ItemSortBy.PREMIERE_DATE, SortOrder.DESCENDING));
            sortOptions.put(3, new SortOption(getString(R.string.lbl_rating), ItemSortBy.OFFICIAL_RATING, SortOrder.ASCENDING));
            sortOptions.put(4, new SortOption(getString(R.string.lbl_community_rating), ItemSortBy.COMMUNITY_RATING, SortOrder.DESCENDING));
            sortOptions.put(5, new SortOption(getString(R.string.lbl_critic_rating), ItemSortBy.CRITIC_RATING, SortOrder.DESCENDING));

            if (isTvShows) {
                sortOptions.put(6, new SortOption(getString(R.string.lbl_last_played), ItemSortBy.SERIES_DATE_PLAYED, SortOrder.DESCENDING));
                sortOptions.put(7, new SortOption(getString(R.string.lbl_date_episode_added), ItemSortBy.DATE_LAST_CONTENT_ADDED, SortOrder.DESCENDING));
            } else {
                sortOptions.put(6, new SortOption(getString(R.string.lbl_last_played), ItemSortBy.DATE_PLAYED, SortOrder.DESCENDING));
                if (mFolder.getCollectionType() == CollectionType.MOVIES) {
                    sortOptions.put(7, new SortOption(getString(R.string.lbl_runtime), ItemSortBy.RUNTIME, SortOrder.ASCENDING));
                }
            }
        }

        setDefaultGridRowCols(mPosterSizeSetting, mImageType);
        setAutoCardGridValues();
        setupQueries();
        setupEventListeners();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = HorizontalGridBrowseBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // KFIN: the library grid keeps only the top edge fade, lifted a bit
        if (binding.rowsFragment instanceof org.jellyfin.androidtv.ui.shared.EdgeFadeFrameLayout) {
            org.jellyfin.androidtv.ui.shared.EdgeFadeFrameLayout fade =
                    (org.jellyfin.androidtv.ui.shared.EdgeFadeFrameLayout) binding.rowsFragment;
            // KFIN: no edge fades on the library grid
            fade.setBottomFadeEnabled(false);
            fade.setTopFade(0f);
        }

        // KFIN: the description is a focus stop between the grid and the status bar
        binding.overview.setFocusable(true);
        binding.overview.setFocusableInTouchMode(true);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            binding.overview.setDefaultFocusHighlightEnabled(false);
        }
        binding.overview.setOnFocusChangeListener((v, hasFocus) ->
                v.setBackgroundColor(hasFocus ? 0x26FFFFFF : android.graphics.Color.TRANSPARENT));
        binding.overview.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == android.view.KeyEvent.ACTION_DOWN
                    && keyCode == android.view.KeyEvent.KEYCODE_DPAD_UP) {
                // KFIN: up from the description lands on the current library's tab
                org.jellyfin.androidtv.ui.shared.toolbar.CatalogBarFocus.INSTANCE.focusTab();
                return true;
            }
            return false;
        });

        binding.getRoot().getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                binding.getRoot().getViewTreeObserver().removeOnGlobalLayoutListener(this);

                // KFIN: compute on pre-draw so the description's bottom reflects
                // the new title position - the grid starts right below it
                binding.getRoot().getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        if (binding == null) {
                            binding.getRoot().getViewTreeObserver().removeOnPreDrawListener(this);
                            return true;
                        }
                        binding.getRoot().getViewTreeObserver().removeOnPreDrawListener(this);
                        // KFIN: the grid starts right below the status bar (30px gap)
                        final int gridTop;
                        float barBottom = org.jellyfin.androidtv.ui.shared.toolbar.CatalogBarFocus.INSTANCE.getSettingsButtonBottomY();
                        if (barBottom >= 0) {
                            gridTop = (int) (barBottom + 30);
                        } else {
                            gridTop = Utils.convertDpToPixel(requireContext(), 80);
                        }
                        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) binding.rowsFragment.getLayoutParams();
                        // KFIN: the clip boundary sits 15px higher so the fade never covers the cards
                        lp.topMargin = gridTop - binding.infoRow.getBottom() - 15;
                        binding.rowsFragment.setLayoutParams(lp);

                        createGrid();
                        loadGrid();
                        addTools();
                        gridInitialized = true;

                        // KFIN: restore the card that was focused before
                        // navigating deeper - waits until the adapter actually
                        // holds that card, otherwise focus lands on the status bar
                        if (savedGridPosition >= 0 && mGridView != null) {
                            final int restore = savedGridPosition;
                            savedGridPosition = -1;
                            restoreGridFocus(restore, 0);
                        }
                        return true;
                    }
                });
            }
        });
    }

    // KFIN: focus the exact card we came from, retrying while the grid loads
    private void restoreGridFocus(int position, int attempts) {
        if (mGridView == null || mAdapter == null || attempts > 15) return;
        if (position >= mAdapter.size()) {
            mGridView.postDelayed(() -> restoreGridFocus(position, attempts + 1), 100);
            return;
        }
        mGridView.setSelectedPosition(position);
        mGridView.post(() -> {
            if (mGridView == null) return;
            androidx.recyclerview.widget.RecyclerView.ViewHolder vh =
                    mGridView.findViewHolderForAdapterPosition(position);
            if (vh != null) {
                vh.itemView.requestFocus();
            } else if (attempts < 15) {
                mGridView.postDelayed(() -> restoreGridFocus(position, attempts + 1), 100);
            } else {
                mGridView.requestFocus();
            }
        });
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_UP) return false;
        return keyProcessor.getValue().handleKey(keyCode, mCurrentItem, mActivity);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // KFIN: remember the focused card so BACK from a details page returns
        // the cursor to it
        if (mGridView != null) {
            savedGridPosition = mGridView.getSelectedPosition();
        }

        BrowseGridTools.INSTANCE.hide();
        binding = null;
        mGridView = null;
        gridInitialized = false;
    }

    private void createGrid() {
        mGridViewHolder = mGridPresenter.onCreateViewHolder(binding.rowsFragment);
        // KFIN: grid geometry is calculated in dp - convert to pixels before
        // handing it to the grid view, otherwise the gaps are density-times
        // smaller than intended (cards appear to touch)
        int padLeft = Utils.convertDpToPixel(requireContext(), mGridPaddingLeft);
        int padTop = Utils.convertDpToPixel(requireContext(), mGridPaddingTop);
        if (mGridViewHolder instanceof HorizontalGridPresenter.ViewHolder) {
            mGridView = ((HorizontalGridPresenter.ViewHolder) mGridViewHolder).getGridView();
            mGridView.setGravity(Gravity.CENTER_VERTICAL);
            ViewGroup.MarginLayoutParams titleMargin = (ViewGroup.MarginLayoutParams) binding.title.getLayoutParams();
            ViewGroup.MarginLayoutParams clockMargin = (ViewGroup.MarginLayoutParams) binding.clock.getLayoutParams();
            mGridView.setPadding(titleMargin.getMarginStart(), padTop, clockMargin.getMarginEnd(), padTop); // prevent initial card cutoffs
        } else if (mGridViewHolder instanceof VerticalGridPresenter.ViewHolder) {
            mGridView = ((VerticalGridPresenter.ViewHolder) mGridViewHolder).getGridView();
            mGridView.setGravity(Gravity.CENTER_HORIZONTAL);
            mGridView.setPadding(padLeft, padTop, padLeft, padTop); // prevent initial card cutoffs
        }
        // KFIN: the grid spans the full width; card sizes already reserve room
        // for the gaps, so the fixed-width cards plus gaps fit exactly
        int spacingPx = Utils.convertDpToPixel(requireContext(), mGridItemSpacingHorizontal);
        ViewGroup.LayoutParams gridLp = mGridView.getLayoutParams();
        if (gridLp == null) {
            gridLp = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            mGridView.setLayoutParams(gridLp);
        } else {
            gridLp.width = ViewGroup.LayoutParams.MATCH_PARENT;
        }
        mGridView.setHorizontalSpacing(spacingPx);
        mGridView.setVerticalSpacing(Utils.convertDpToPixel(requireContext(), mGridItemSpacingVertical));
        mGridView.setFocusable(true);
        binding.rowsFragment.removeAllViews();
        binding.rowsFragment.addView(mGridViewHolder.view);

        updateAdapter();
    }

    private void updateAdapter() {
        if (mGridView != null) {
            mGridPresenter.onBindViewHolder(mGridViewHolder, mAdapter);
            if (mSelectedPosition != -1) {
                mGridView.setSelectedPosition(mSelectedPosition);
            }
        }
    }

    /**
     * Sets the grid presenter.
     */
    public void setGridPresenter(HorizontalGridPresenter gridPresenter) {
        if (gridPresenter == null) {
            throw new IllegalArgumentException("Grid presenter may not be null");
        }
        gridPresenter.setOnItemViewSelectedListener(mRowSelectedListener);
        gridPresenter.setOnItemViewClickedListener(mClickedListener);
        gridPresenter.setShadowEnabled(false);
        mGridPresenter = gridPresenter;
    }

    /**
     * Sets the grid presenter.
     */
    public void setGridPresenter(VerticalGridPresenter gridPresenter) {
        if (gridPresenter == null) {
            throw new IllegalArgumentException("Grid presenter may not be null");
        }
        gridPresenter.setOnItemViewSelectedListener(mRowSelectedListener);
        gridPresenter.setOnItemViewClickedListener(mClickedListener);
        gridPresenter.setShadowEnabled(false);
        mGridPresenter = gridPresenter;
    }


    public void setItem(BaseRowItem item) {
        if (item != null) {
            binding.title.setText(item.getFullName(requireContext()));
            InfoLayoutHelper.addInfoRow(requireContext(), item.getBaseItem(), binding.infoRow, true);
            binding.overview.setText(item.getBaseItem() != null ? item.getBaseItem().getOverview() : null);
            // KFIN: item logo in the top-right corner, over the sharp backdrop
            String logoUrl = item.getBaseItem() != null
                    ? imageHelper.getValue().getLogoImageUrl(item.getBaseItem(), 600)
                    : null;
            // KFIN: the library logo is hidden - keep it gone
            binding.itemLogo.setVisibility(View.GONE);
        } else {
            binding.title.setText("");
            binding.infoRow.removeAllViews();
            binding.overview.setText(null);
            binding.itemLogo.setVisibility(View.GONE);
        }
    }

    public class SortOption {
        public String name;
        public ItemSortBy value;
        public SortOrder order;

        public SortOption(String name, ItemSortBy value, SortOrder order) {
            this.name = name;
            this.value = value;
            this.order = order;
        }
    }

    private Map<Integer, SortOption> sortOptions;

    private SortOption getSortOption(ItemSortBy value) {
        for (SortOption sortOption : sortOptions.values()) {
            if (sortOption.value.equals(value)) return sortOption;
        }

        return new SortOption(getString(R.string.lbl_bracket_unknown), ItemSortBy.SORT_NAME, SortOrder.ASCENDING);
    }

    public void setStatusText(String folderName) {
        String text = getString(R.string.lbl_showing) + " ";
        FilterOptions filters = mAdapter.getFilters();
        if (filters == null || (!filters.isFavoriteOnly() && !filters.isUnwatchedOnly())) {
            text += getString(R.string.lbl_all_items);
        } else {
            text += (filters.isUnwatchedOnly() ? getString(R.string.lbl_unwatched) : "") + " " +
                    (filters.isFavoriteOnly() ? getString(R.string.lbl_favorites) : "");
        }

        if (mAdapter.getStartLetter() != null) {
            text += " " + getString(R.string.lbl_starting_with) + " " + mAdapter.getStartLetter();
        }

        text += " " + getString(R.string.lbl_from) + " '" + folderName + "' " + getString(R.string.lbl_sorted_by) + " " + getSortOption(mAdapter.getSortBy()).name;

        // KFIN: status line removed from the screen
    }

    final private OnItemViewSelectedListener mRowSelectedListener =
            new OnItemViewSelectedListener() {
                @Override
                public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item,
                                           RowPresenter.ViewHolder rowViewHolder, Row row) {
                    int position = mGridView.getSelectedPosition();
                    Timber.d("row selected position %s", position);
                    if (position != mSelectedPosition) {
                        mSelectedPosition = position;
                    }
                    // Update the counter
                    updateCounter(position + 1);
                    if (position >= 0) {
                        mSelectedListener.onItemSelected(itemViewHolder, item, rowViewHolder, row);
                    }
                }
            };

    public void updateCounter(int position) {
        if (mAdapter != null) {
            binding.counter.setText(MessageFormat.format("{0} | {1}", position, mAdapter.getTotalItems()));
        }
    }

    private void setRowDef(final BrowseRowDef rowDef) {
        if (mRowDef == null || mRowDef.hashCode() != rowDef.hashCode()) {
            mDirty = true;
        }
        mRowDef = rowDef;
    }

    private double getCardWidthBy(final double cardHeight, ImageType imageType, BaseItemDto folder) {
        switch (imageType) {
            case POSTER:
                // special handling for square posters
                BaseItemKind fType = folder.getType();
                if (fType == BaseItemKind.AUDIO || fType == BaseItemKind.GENRE || fType == BaseItemKind.MUSIC_ALBUM || fType == BaseItemKind.MUSIC_ARTIST || fType == BaseItemKind.MUSIC_GENRE) {
                    return cardHeight;
                } else if (fType == BaseItemKind.COLLECTION_FOLDER && CollectionType.MUSIC.equals(folder.getCollectionType())) {
                    return cardHeight;
                } else {
                    return cardHeight * ImageHelper.ASPECT_RATIO_2_3;
                }
            case THUMB:
                return cardHeight * ImageHelper.ASPECT_RATIO_16_9;
            case BANNER:
                return cardHeight * ImageHelper.ASPECT_RATIO_BANNER;
            default:
                throw new IllegalStateException("Unexpected value: " + imageType);
        }
    }

    private double getCardHeightBy(final double cardWidth, ImageType imageType, BaseItemDto folder) {
        switch (imageType) {
            case POSTER:
                // special handling for square posters
                BaseItemKind fType = folder.getType();
                if (fType == BaseItemKind.AUDIO || fType == BaseItemKind.GENRE || fType == BaseItemKind.MUSIC_ALBUM || fType == BaseItemKind.MUSIC_ARTIST || fType == BaseItemKind.MUSIC_GENRE) {
                    return cardWidth;
                } else if (fType == BaseItemKind.COLLECTION_FOLDER && CollectionType.MUSIC.equals(folder.getCollectionType())) {
                    return cardWidth;
                } else {
                    return cardWidth / ImageHelper.ASPECT_RATIO_2_3;
                }
            case THUMB:
                return cardWidth / ImageHelper.ASPECT_RATIO_16_9;
            case BANNER:
                return cardWidth / ImageHelper.ASPECT_RATIO_BANNER;
            default:
                throw new IllegalArgumentException("Unexpected value: " + imageType);
        }
    }

    private void setDefaultGridRowCols(PosterSize posterSize, ImageType imageType) {
        // HINT: use uneven Rows/Cols if possible, so selected middle lines up with TV middle!
        if (mGridPresenter instanceof VerticalGridPresenter) {
            int numCols;
            switch (posterSize) {
                case SMALLEST:
                    numCols = imageType.equals(ImageType.BANNER) ? 6 : imageType.equals(ImageType.THUMB) ? 11 : 15;
                    break;
                case SMALL:
                    numCols = imageType.equals(ImageType.BANNER) ? 5 : imageType.equals(ImageType.THUMB) ? 9 : 13;
                    break;
                case MED:
                    numCols = imageType.equals(ImageType.BANNER) ? 4 : imageType.equals(ImageType.THUMB) ? 7 : 11;
                    break;
                case LARGE:
                    numCols = imageType.equals(ImageType.BANNER) ? 3 : imageType.equals(ImageType.THUMB) ? 5 : 7;
                    break;
                case X_LARGE:
                    numCols = imageType.equals(ImageType.BANNER) ? 2 : imageType.equals(ImageType.THUMB) ? 3 : 5;
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + mPosterSizeSetting);
            }
            ((VerticalGridPresenter) mGridPresenter).setNumberOfColumns(numCols);
        } else if (mGridPresenter instanceof HorizontalGridPresenter) {
            int numRows;
            switch (posterSize) {
                case SMALLEST:
                    numRows = imageType.equals(ImageType.BANNER) ? 13 : imageType.equals(ImageType.THUMB) ? 7 : 5;
                    break;
                case SMALL:
                    numRows = imageType.equals(ImageType.BANNER) ? 11 : imageType.equals(ImageType.THUMB) ? 6 : 4;
                    break;
                case MED:
                    numRows = imageType.equals(ImageType.BANNER) ? 9 : imageType.equals(ImageType.THUMB) ? 5 : 3;
                    break;
                case LARGE:
                    numRows = imageType.equals(ImageType.BANNER) ? 7 : imageType.equals(ImageType.THUMB) ? 4 : 2;
                    break;
                case X_LARGE:
                    numRows = imageType.equals(ImageType.BANNER) ? 5 : imageType.equals(ImageType.THUMB) ? 2 : 1;
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + mPosterSizeSetting);
            }
            ((HorizontalGridPresenter) mGridPresenter).setNumberOfRows(numRows);
        }
    }

    private void setAutoCardGridValues() {
        if (mGridPresenter == null) {
            Timber.e("Invalid presenter, cant calculate CardGridValues!");
            return;
        }
        double cardScaling = Math.max(mCardFocusScale - 1.0, 0.0);
        int cardHeightInt = 100;
        int spacingHorizontalInt = 0;
        int spacingVerticalInt = 0;
        int paddingLeftInt = 0;
        int paddingTopInt = 0;
        int numRows = 0;
        int numCols = 0;
        int numCardsScreen = 0; // number of cards visible, including cutoff's

        if (mGridPresenter instanceof HorizontalGridPresenter) {
            numRows = ((HorizontalGridPresenter) mGridPresenter).getNumberOfRows();
            if (numRows == 1) { // reduce size so minimal cards are shown
                numRows = 0;
                numCols = MIN_NUM_CARDS;
            }
        } else if (mGridPresenter instanceof VerticalGridPresenter) {
            numCols = ((VerticalGridPresenter) mGridPresenter).getNumberOfColumns();
        }

        if (numRows > 0) {
            double paddingPct = cardScaling / numRows;
            double spacingPct = ((paddingPct / 2.0) * CARD_SPACING_PCT) * (numRows - 1);

            double wastedSpacePct = paddingPct + spacingPct;
            double usableCardSpace = mGridHeight / (1.0 + wastedSpacePct); // decrease size
            double cardHeight = usableCardSpace / numRows;

            // fix any rounding errors and make pixel perfect
            cardHeightInt = (int) Math.round(cardHeight);
            double cardPaddingTopBottomAdj = cardHeightInt * cardScaling;
            spacingVerticalInt = Math.max((int) (Math.round((cardPaddingTopBottomAdj / 2.0) * CARD_SPACING_PCT)), 0); // round spacing
            int paddingTopBottomInt = mGridHeight - ((cardHeightInt * numRows) + (spacingVerticalInt * (numRows - 1)));
            paddingTopInt = Math.max(paddingTopBottomInt / 2, 0);

            int sumSize = (cardHeightInt * numRows) + (spacingVerticalInt * (numRows - 1)) + (paddingTopInt * 2);
            if (Math.abs(sumSize - mGridHeight) > 2) {
                Timber.w("setAutoCardGridValues calculation delta > 2, something is off GridHeight <%s> sumSize <%s>!", mGridHeight, sumSize);
            }
            int cardWidthInt = (int) getCardWidthBy(cardHeightInt, mImageType, mFolder);
            paddingLeftInt = (int) Math.round((cardWidthInt * cardScaling) / 2.0);
            spacingHorizontalInt = Math.max((int) (Math.round(paddingLeftInt * CARD_SPACING_PCT)), 0); // round spacing
            if (mImageType == ImageType.BANNER) {
                spacingHorizontalInt = Math.max((int) (Math.round(paddingLeftInt * CARD_SPACING_HORIZONTAL_BANNER_PCT)), 0); // round spacing
            }
            int cardsCol = (int) Math.round(((double) mGridWidth / (cardWidthInt + spacingHorizontalInt)) + 0.5);
            mCardsScreenEst = numRows * cardsCol;
            mCardsScreenStride = numRows;
        } else if (numCols > 0) {
            // KFIN: grid spans edge to edge; reserve 10% of the width for the
            // gaps up-front so the fixed-width cards plus the gaps always fit
            // exactly (Leanback scrolls the grid horizontally otherwise)
            int gapDp = (int) Math.round((mGridWidth * 0.10) / Math.max(numCols - 1, 1));
            double usableCardSpace = mGridWidth - (gapDp * (numCols - 1));
            double cardWidth = usableCardSpace / numCols;

            cardHeightInt = (int) Math.round(getCardHeightBy(cardWidth, mImageType, mFolder));
            int cardWidthInt = (int) getCardWidthBy(cardHeightInt, mImageType, mFolder);

            // KFIN: all reserved space goes into the gaps between cards
            int totalGapSpace = Math.max(mGridWidth - (cardWidthInt * numCols), 0);
            int gap = totalGapSpace / Math.max(numCols - 1, 1);
            spacingHorizontalInt = gap;
            paddingLeftInt = 0; // grid starts at the screen edge

            int sumSize = (cardWidthInt * numCols) + (spacingHorizontalInt * (numCols - 1)) + (paddingLeftInt * 2);
            if (Math.abs(sumSize - mGridWidth) > numCols + 2) {
                Timber.w("setAutoCardGridValues calculation delta too large, something is off GridWidth <%s> sumSize <%s>!", mGridWidth, sumSize);
            }
            paddingTopInt = (int) Math.round((cardHeightInt * cardScaling) / 2.0);
            // KFIN: vertical gap matches the horizontal gap between cards
            spacingVerticalInt = spacingHorizontalInt;
            int cardsRow = (int) Math.round(((double) mGridHeight / (cardHeightInt + spacingVerticalInt)) + 0.5);
            mCardsScreenEst = numCols * cardsRow;
            mCardsScreenStride = numCols;
        }

        Timber.d("numCardsScreen <%s>", numCardsScreen);

        if (mCardHeight != cardHeightInt) {
            mDirty = true;
        }
        mCardHeight = cardHeightInt;
        mGridItemSpacingHorizontal = spacingHorizontalInt;
        mGridItemSpacingVertical = spacingVerticalInt;
        mGridPaddingLeft = paddingLeftInt;
        mGridPaddingTop = paddingTopInt;
    }

    private void setupQueries() {
        if (getArguments().getBoolean(Extras.Favorites, false)) {
            setRowDef(new BrowseRowDef("", BrowsingUtils.createFavoritesRequest(), CHUNK_SIZE_MINIMUM, new ChangeTriggerType[]{}));
            return;
        }

        if (mFolder.getType() == BaseItemKind.USER_VIEW || mFolder.getType() == BaseItemKind.COLLECTION_FOLDER) {
            CollectionType type = mFolder.getCollectionType() != null ? mFolder.getCollectionType() : CollectionType.UNKNOWN;
            if (type == CollectionType.MUSIC) {
                //Special queries needed for album artists
                String includeType = getArguments().getString(Extras.IncludeType, null);
                if ("AlbumArtist".equals(includeType)) {
                    setRowDef(new BrowseRowDef("", BrowsingUtils.createAlbumArtistsRequest(mParentId), CHUNK_SIZE_MINIMUM, new ChangeTriggerType[]{}));
                    return;
                } else if ("Artist".equals(includeType)) {
                    setRowDef(new BrowseRowDef("", BrowsingUtils.createArtistsRequest(mParentId), CHUNK_SIZE_MINIMUM, new ChangeTriggerType[]{}));
                    return;
                }
            }
        }

        setRowDef(new BrowseRowDef("", BrowsingUtils.createBrowseGridItemsRequest(mFolder), CHUNK_SIZE_MINIMUM, false, true));
    }

    @Override
    public void onPause() {
        super.onPause();
        // KFIN: also clear the status-bar tools here; some exit paths
        // (e.g. back to home) keep the view alive without onDestroyView
        BrowseGridTools.INSTANCE.hide();
        ContentPrimaryAction.INSTANCE.setPrimary(null);
        // KFIN: leaving the library - other screens may load their backdrop
        backgroundService.getValue().setGradientBackground(false);
    }

    @Override
    public void onResume() {
        super.onResume();
        // KFIN: no backdrop while the library is on screen - entering it and
        // returning from an item must both render plain black
        backgroundService.getValue().setGradientBackground(true);
        // KFIN: tools were hidden in onPause; re-register them when coming back
        // (e.g. after viewing an item and pressing back)
        addTools();
        // KFIN: DOWN from any status bar button lands on the leftmost card of the grid
        ContentPrimaryAction.INSTANCE.setPrimary(() -> {
            if (mGridView != null) {
                if (mGridPresenter instanceof VerticalGridPresenter) {
                    int cols = ((VerticalGridPresenter) mGridPresenter).getNumberOfColumns();
                    int pos = mGridView.getSelectedPosition();
                    if (cols > 0 && pos >= 0) mGridView.setSelectedPosition(pos - (pos % cols));
                }
                mGridView.requestFocus();
            }
        });
        // Use the poster size captured in onCreate for the whole lifetime of this
        // screen; re-reading it here picks up late server-side display-preference
        // syncs, which rebuilds the grid with different card geometry mid-session
        // and makes focus jump between columns while scrolling
        PosterSize posterSizeSetting = mPosterSizeSetting;
        ImageType imageType = ImageType.POSTER;
        GridDirection gridDirection = GridDirection.VERTICAL;

        if (mImageType != imageType || mPosterSizeSetting != posterSizeSetting || mGridDirection != gridDirection || mDirty) {
            determiningPosterSize = true;

            mImageType = imageType;
            mPosterSizeSetting = posterSizeSetting;
            mGridDirection = gridDirection;

            if (mGridDirection.equals(GridDirection.VERTICAL) && (mGridPresenter == null || !(mGridPresenter instanceof VerticalGridPresenter))) {
                setGridPresenter(new VerticalGridPresenter(FocusHighlight.ZOOM_FACTOR_LARGE, false));
            } else if (mGridDirection.equals(GridDirection.HORIZONTAL) && (mGridPresenter == null || !(mGridPresenter instanceof HorizontalGridPresenter))) {
                setGridPresenter(new HorizontalGridPresenter());
            }
            setDefaultGridRowCols(mPosterSizeSetting, mImageType);
            setAutoCardGridValues();
            if (gridInitialized) {
                // First creation is deferred to the global layout listener so the
                // grid is built exactly once with the final measured geometry
                createGrid();
                loadGrid();
            }
            determiningPosterSize = false;
        }

        if (!justLoaded) {
            //Re-retrieve anything that needs it but delay slightly so we don't take away gui landing
            if (mAdapter != null) {
                mHandler.postDelayed(() -> {
                    if (!getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED))
                        return;

                    if (mAdapter != null && mAdapter.size() > 0) {
                        if (!mAdapter.ReRetrieveIfNeeded()) {
                            refreshCurrentItem();
                        }
                    }
                }, 500);
            }
        } else {
            justLoaded = false;
        }
    }

    private void buildAdapter() {
        mCardPresenter = new CardPresenter(false, mImageType, mCardHeight, true);

        Timber.d("buildAdapter cardHeight <%s> getCardWidthBy <%s> chunks <%s> type <%s>", mCardHeight, (int) getCardWidthBy(mCardHeight, mImageType, mFolder), mRowDef.getChunkSize(), mRowDef.getQueryType().toString());

        // adapt chunk size if needed
        int chunkSize = mRowDef.getChunkSize();
        if (mCardsScreenEst > 0 && mCardsScreenEst >= chunkSize) {
            chunkSize = Math.min(mCardsScreenEst + mCardsScreenStride, 150); // cap at 150
            Timber.d("buildAdapter adjusting chunkSize to <%s> screenEst <%s>", chunkSize, mCardsScreenEst);
        }
        chunkSize = 10000; // load the entire library in one request to avoid paging glitches while scrolling

        switch (mRowDef.getQueryType()) {
            case NextUp:
                mAdapter = new ItemRowAdapter(requireContext(), mRowDef.getNextUpQuery(), true, mCardPresenter, null);
                break;
            case Views:
                mAdapter = new ItemRowAdapter(requireContext(), GetUserViewsRequest.INSTANCE, mCardPresenter, null);
                break;
            case SimilarSeries:
                mAdapter = new ItemRowAdapter(requireContext(), mRowDef.getSimilarQuery(), QueryType.SimilarSeries, mCardPresenter, null);
                break;
            case SimilarMovies:
                mAdapter = new ItemRowAdapter(requireContext(), mRowDef.getSimilarQuery(), QueryType.SimilarMovies, mCardPresenter, null);
                break;
            case LiveTvChannel:
                mAdapter = new ItemRowAdapter(requireContext(), mRowDef.getTvChannelQuery(), 40, mCardPresenter, null);
                break;
            case LiveTvProgram:
                mAdapter = new ItemRowAdapter(requireContext(), mRowDef.getProgramQuery(), mCardPresenter, null);
                break;
            case LiveTvRecording:
                mAdapter = new ItemRowAdapter(requireContext(), mRowDef.getRecordingQuery(), chunkSize, mCardPresenter, null);
                break;
            case Artists:
                mAdapter = new ItemRowAdapter(requireContext(), mRowDef.getArtistsQuery(), chunkSize, mCardPresenter, null);
                break;
            case AlbumArtists:
                mAdapter = new ItemRowAdapter(requireContext(), mRowDef.getAlbumArtistsQuery(), chunkSize, mCardPresenter, null);
                break;
            default:
                mAdapter = new ItemRowAdapter(requireContext(), mRowDef.getQuery(), chunkSize, mRowDef.getPreferParentThumb(), mRowDef.isStaticHeight(), mCardPresenter, null);
                break;
        }
        mDirty = false;
        mLastLoadSignature = null; // new adapter must fetch

        FilterOptions filters = new FilterOptions();
        filters.setFavoriteOnly(libraryPreferences.get(LibraryPreferences.Companion.getFilterFavoritesOnly()));
        filters.setUnwatchedOnly(libraryPreferences.get(LibraryPreferences.Companion.getFilterUnwatchedOnly()));

        mAdapter.setRetrieveFinishedListener(new EmptyResponse(getLifecycle()) {
            @Override
            public void onResponse() {
                if (!isActive()) return;
                setStatusText(mFolder.getName());
                if (mCurrentItem == null) { // don't mess-up pos via loadMoreItemsIfNeeded
                    setItem(null);
                    updateCounter(mAdapter.getTotalItems() > 0 ? 1 : 0);
                }
                // KFIN: letter jump lives in the status bar and is always available
                if (mAdapter.getItemsLoaded() == 0) {
                    mGridView.setFocusable(false);
                    mHandler.postDelayed(() -> {
                        if (!getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED))
                            return;

                        binding.title.setText(mFolder.getName());
                    }, 500);
                } else if (mGridView != null) {
                    mGridView.setFocusable(true);
                    mGridView.requestFocus();
                }
            }
        });
        mAdapter.setFilters(filters);

        updateAdapter();
    }

    public void loadGrid() {
        if (mCardPresenter == null || mAdapter == null || mDirty) {
            buildAdapter();
        }

        mAdapter.setSortBy(getSortOption(libraryPreferences.get(LibraryPreferences.Companion.getSortBy())));

        String signature = mAdapter.getQuerySignature();
        if (signature.equals(mLastLoadSignature)
                && mAdapter.getTotalItems() > 0
                && mAdapter.getItemsLoaded() >= mAdapter.getTotalItems()) {
            // Already fully loaded with this exact query; re-retrieving would rebuild
            // the grid and make the content appear to reshuffle
            return;
        }
        mLastLoadSignature = signature;
        mAdapter.Retrieve();
    }

    private JumplistPopup jumplistPopup;

    public FilterOptions getFiltersSafe() {
        FilterOptions filters = mAdapter != null ? mAdapter.getFilters() : null;
        return filters != null ? filters : new FilterOptions();
    }

    public boolean isItemsQuery() {
        return mRowDef != null && mRowDef.getQueryType() == QueryType.Items;
    }

    public void openSortMenu() {
        // KFIN: centered popup dropping from below the status bar, capsule focus style
        new SortPopup().show();
    }

    private class SortPopup {
        private final PopupWindow popupWindow;
        private TextView checkedView;

        SortPopup() {
            LinearLayout container = (LinearLayout) getLayoutInflater().inflate(R.layout.kfin_popup_list, binding.rowsFragment, false);
            // KFIN: status-bar style matte glass capsule, 90%
            GradientDrawable capsule = new GradientDrawable();
            capsule.setShape(GradientDrawable.RECTANGLE);
            capsule.setColor(0xFF000000);
            capsule.setCornerRadius(Utils.convertDpToPixel(requireContext(), 24));
            capsule.setStroke(Utils.convertDpToPixel(requireContext(), 1), 0x24FFFFFF);
            container.setBackground(capsule);

            final int hPad = Utils.convertDpToPixel(requireContext(), 16);
            final int vPad = Utils.convertDpToPixel(requireContext(), 10);
            final int pillRadius = Utils.convertDpToPixel(requireContext(), 40);
            final int focusedStroke = Utils.convertDpToPixel(requireContext(), 1);
            final Object currentSort = libraryPreferences.get(LibraryPreferences.Companion.getSortBy());

            popupWindow = new PopupWindow(container, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
            popupWindow.setOutsideTouchable(true);
            popupWindow.setAnimationStyle(R.style.WindowAnimation_Fade);

            for (Map.Entry<Integer, SortOption> entry : sortOptions.entrySet()) {
                final SortOption option = entry.getValue();
                boolean checked = option.value.equals(currentSort);
                TextView item = new TextView(requireContext());
                // KFIN: uppercase like the rest of the UI labels
                item.setAllCaps(true);
                item.setText(checked ? "\u2713  " + option.name : option.name);
                item.setTextColor(Color.WHITE);
                item.setTextSize(14);
                item.setPadding(hPad, vPad, hPad, vPad);
                item.setFocusable(true);
                item.setFocusableInTouchMode(true);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    item.setDefaultFocusHighlightEnabled(false);
                }

                // KFIN: unified focus style - white outline only
                final GradientDrawable itemBg = new GradientDrawable();
                itemBg.setShape(GradientDrawable.RECTANGLE);
                itemBg.setCornerRadius(pillRadius);
                itemBg.setStroke(0, 0x00000000);
                item.setBackground(itemBg);
                item.setOnFocusChangeListener((v, hasFocus) -> {
                    // KFIN: watch-button style - 80% white fill, dark text
                    itemBg.setColor(hasFocus ? 0xFFFFFFFF : 0x00000000);
                    itemBg.setStroke(hasFocus ? focusedStroke : 0, hasFocus ? 0xFFFFFFFF : 0x00000000);
                    item.setTextColor(hasFocus ? 0xE6000000 : Color.WHITE);
                });

                item.setOnClickListener(v -> {
                    mAdapter.setSortBy(option);
                    mAdapter.Retrieve();
                    updateDisplayPrefs();
                    popupWindow.dismiss();
                });
                container.addView(item);
                if (checked) checkedView = item;
            }
        }

        void show() {
            // KFIN: top edge 20px below the status bar, the menu's axis on the
            // sort button's axis
            popupWindow.getContentView().measure(
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            int width = popupWindow.getContentView().getMeasuredWidth();
            float centerX = org.jellyfin.androidtv.ui.browsing.BrowseGridTools.INSTANCE.getSortButtonCenterX();
            float belowY = org.jellyfin.androidtv.ui.browsing.BrowseGridTools.INSTANCE.getSortButtonBottomY();
            int x = centerX >= 0 ? (int) (centerX - width / 2f) : 0;
            int y = belowY >= 0 ? (int) (belowY + 20) : Utils.convertDpToPixel(requireContext(), 76);
            popupWindow.showAtLocation(binding.getRoot(), Gravity.TOP | Gravity.START, x, y);
            if (checkedView != null) checkedView.requestFocus();
        }
    }

    public void applyUnwatchedFilter(boolean unwatchedOnly) {
        FilterOptions filters = getFiltersSafe();
        filters.setUnwatchedOnly(unwatchedOnly);
        mAdapter.setFilters(filters);
        mAdapter.Retrieve();
        updateDisplayPrefs();
    }

    public void applyFavoriteFilter(boolean favoriteOnly) {
        FilterOptions filters = getFiltersSafe();
        filters.setFavoriteOnly(favoriteOnly);
        mAdapter.setFilters(filters);
        mAdapter.Retrieve();
        updateDisplayPrefs();
    }

    public void openJumplist() {
        if (jumplistPopup != null) jumplistPopup.show();
    }

    private void updateDisplayPrefs() {
        CoroutineUtils.runOnLifecycle(getLifecycle(), (coroutineScope, continuation) -> {
            libraryPreferences.set(LibraryPreferences.Companion.getFilterFavoritesOnly(), mAdapter.getFilters().isFavoriteOnly());
            libraryPreferences.set(LibraryPreferences.Companion.getFilterUnwatchedOnly(), mAdapter.getFilters().isUnwatchedOnly());
            libraryPreferences.set(LibraryPreferences.Companion.getSortBy(), mAdapter.getSortBy());
            libraryPreferences.set(LibraryPreferences.Companion.getSortOrder(), getSortOption(mAdapter.getSortBy()).order);
            return libraryPreferences.commit(continuation);
        });
    }

    private void addTools() {
        jumplistPopup = new JumplistPopup();
        // KFIN: tools live in the status bar, not in the page toolbar
        BrowseGridTools.INSTANCE.show(this);
    }

    class JumplistPopup {
        private final int WIDTH = Utils.convertDpToPixel(requireContext(), 900);
        private final int HEIGHT = Utils.convertDpToPixel(requireContext(), 55);

        private final PopupWindow popupWindow;
        private final AlphaPickerView alphaPicker;

        JumplistPopup() {
            PopupEmptyBinding layout = PopupEmptyBinding.inflate(getLayoutInflater(), binding.rowsFragment, false);
            // KFIN: status-bar style matte glass, 90%
            GradientDrawable capsule = new GradientDrawable();
            capsule.setShape(GradientDrawable.RECTANGLE);
            capsule.setColor(0xFF000000);
            capsule.setCornerRadius(Utils.convertDpToPixel(requireContext(), 40));
            capsule.setStroke(Utils.convertDpToPixel(requireContext(), 1), 0x24FFFFFF);
            layout.emptyPopup.setBackground(capsule);
            popupWindow = new PopupWindow(layout.emptyPopup, WIDTH, HEIGHT, true);
            popupWindow.setOutsideTouchable(true);
            popupWindow.setAnimationStyle(R.style.WindowAnimation_Fade);

            alphaPicker = new AlphaPickerView(requireContext(), null);
            alphaPicker.setOnAlphaSelected(letter -> {
                mAdapter.setStartLetter(letter.toString());
                loadGrid();
                dismiss();
                return null;
            });

            layout.emptyPopup.addView(alphaPicker);
        }

        public void show() {
            // KFIN: letters bar sits 20px below the status bar (the jump
            // button's bottom edge), fallback 60dp
            float jumpBottom = org.jellyfin.androidtv.ui.browsing.BrowseGridTools.INSTANCE.getJumpButtonBottomY();
            int y = jumpBottom >= 0 ? (int) (jumpBottom + 20) : Utils.convertDpToPixel(requireContext(), 60);
            popupWindow.showAtLocation(binding.getRoot(), Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, y);
            if (mAdapter.getStartLetter() != null && !mAdapter.getStartLetter().isEmpty()) {
                alphaPicker.focus(mAdapter.getStartLetter().charAt(0));
            }
        }

        public void dismiss() {
            if (popupWindow != null && popupWindow.isShowing()) {
                popupWindow.dismiss();
            }
        }
    }

    private void setupEventListeners() {
        if (mGridPresenter != null) {
            if (mGridPresenter instanceof HorizontalGridPresenter)
                ((HorizontalGridPresenter) mGridPresenter).setOnItemViewClickedListener(mClickedListener);
            else if (mGridPresenter instanceof VerticalGridPresenter)
                ((VerticalGridPresenter) mGridPresenter).setOnItemViewClickedListener(mClickedListener);
        }
        mClickedListener.registerListener(new ItemViewClickedListener());
        mSelectedListener.registerListener(new ItemViewSelectedListener());

        CoroutineUtils.readCustomMessagesOnLifecycle(getLifecycle(), customMessageRepository.getValue(), message -> {
            if (message.equals(CustomMessage.RefreshCurrentItem.INSTANCE)) refreshCurrentItem();
            return null;
        });
    }

    private void refreshCurrentItem() {
        if (mCurrentItem == null) return;
        Timber.i("Refresh item \"%s\"", mCurrentItem.getFullName(requireContext()));
        ItemRowAdapterHelperKt.refreshItem(mAdapter, api.getValue(), this, mCurrentItem, () -> {
            //Now - if filtered make sure we still pass
            if (mAdapter.getFilters() == null) return null;
            if ((mAdapter.getFilters().isFavoriteOnly() && !mCurrentItem.isFavorite()) || (mAdapter.getFilters().isUnwatchedOnly() && mCurrentItem.isPlayed())) {
                // if we are about to remove the current item, throw focus to toolbar so framework doesn't crash
                binding.toolBar.requestFocus();
                mAdapter.remove(mCurrentItem);
                mAdapter.setTotalItems(mAdapter.getTotalItems() - 1);
                updateCounter(mAdapter.indexOf(mCurrentItem));
            }
            return null;
        });
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(final Presenter.ViewHolder itemViewHolder, Object item,
                                  RowPresenter.ViewHolder rowViewHolder, Row row) {

            if (!(item instanceof BaseRowItem)) return;
            itemLauncher.getValue().launch((BaseRowItem) item, mAdapter, requireContext());
        }
    }

    private final Runnable mDelayedSetItem = new Runnable() {
        @Override
        public void run() {
            if (!getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)) return;

            backgroundService.getValue().setBackground(mCurrentItem.getBaseItem());
            setItem(mCurrentItem);
        }
    };

    private final class ItemViewSelectedListener implements OnItemViewSelectedListener {
        @Override
        public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item,
                                   RowPresenter.ViewHolder rowViewHolder, Row row) {
            mHandler.removeCallbacks(mDelayedSetItem);
            if (!(item instanceof BaseRowItem)) {
                mCurrentItem = null;
                binding.title.setText(mainTitle);
                //fill in default background
                backgroundService.getValue().clearBackgrounds();
            } else {
                mCurrentItem = (BaseRowItem) item;
                binding.title.setText(mCurrentItem.getName(requireContext()));
                binding.infoRow.removeAllViews();
                mHandler.postDelayed(mDelayedSetItem, VIEW_SELECT_UPDATE_DELAY);

                if (!determiningPosterSize)
                    mAdapter.loadMoreItemsIfNeeded(mAdapter.indexOf(mCurrentItem));
            }
        }
    }
}
