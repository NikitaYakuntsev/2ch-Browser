package ua.in.quireg.chan.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import ua.in.quireg.chan.R;
import ua.in.quireg.chan.common.utils.StringUtils;
import ua.in.quireg.chan.models.presentation.BoardEntity;
import ua.in.quireg.chan.models.domain.BoardModel;
import ua.in.quireg.chan.models.presentation.IBoardListEntity;
import ua.in.quireg.chan.models.presentation.SectionEntity;

public class BoardsListAdapter extends ArrayAdapter<IBoardListEntity> {
    private static final int ITEM_VIEW_TYPE_BOARD = 0;
    private static final int ITEM_VIEW_TYPE_SEPARATOR = 1;

    private static final int FAVORITES_SECTION_POSITION = 0;

    private final LayoutInflater mInflater;

    private Context mContext;

    private int mFavoritesCount = 0;

    public BoardsListAdapter(Context context) {
        super(context, -1);
        this.mContext = context;
        this.mInflater = LayoutInflater.from(context);
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        return this.getItem(position).isSection() ? ITEM_VIEW_TYPE_SEPARATOR : ITEM_VIEW_TYPE_BOARD;
    }

    @Override
    public boolean isEnabled(int position) {
        return !this.getItem(position).isSection();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        final IBoardListEntity item = this.getItem(position);

        if (convertView == null) {
            convertView = this.mInflater.inflate(item.isSection()
                    ? ua.in.quireg.chan.R.layout.pick_board_section
                    : R.layout.pick_board_board, null);
        }

        if (item.isSection()) {
            SectionEntity si = (SectionEntity) item;

            final TextView sectionView = (TextView) convertView;
            sectionView.setText(si.getTitle());
        } else {
            BoardEntity bi = (BoardEntity) item;
            final TextView boardName = (TextView) convertView.findViewById(R.id.pick_board_name);
            final TextView boardBumpLimit = (TextView) convertView.findViewById(R.id.pick_board_bump_limit);

            String description = !StringUtils.isEmpty(bi.getTitle())
                    ? bi.getCode() + " - " + bi.getTitle()
                    : bi.getCode();
            String bumpLimit = !StringUtils.isEmpty(bi.getBumpLimit())
                    ? bi.getBumpLimit()
                    : "?";

            boardName.setText(description);
            boardBumpLimit.setText(bumpLimit);
        }

        return convertView;
    }

    @Override
    public void clear() {
        super.clear();

        this.mFavoritesCount = 0;
    }

    public void addItemToFavoritesSection(String boardName, BoardModel boardModel) {
        if (this.mFavoritesCount == 0) {
            this.insert(new SectionEntity(this.getContext().getString(R.string.favorites)), FAVORITES_SECTION_POSITION);
        }

        this.mFavoritesCount++;

        BoardEntity newItem;
        if (boardModel != null) {
            newItem = new BoardEntity(boardName,
                    boardModel.getName(),
                    boardModel.getBump_limit() != null ? boardModel.getBump_limit() : "?");
        }else{
            newItem = new BoardEntity(boardName, boardName, "?");
        }


        this.insert(newItem, this.mFavoritesCount);
    }

    public void removeItemFromFavoritesSection(BoardEntity item) {
        this.mFavoritesCount = Math.max(0, this.mFavoritesCount - 1);
        this.remove(item);

        if (this.mFavoritesCount == 0) {
            this.remove(this.getItem(FAVORITES_SECTION_POSITION));
        }

        this.notifyDataSetChanged();
    }
}
