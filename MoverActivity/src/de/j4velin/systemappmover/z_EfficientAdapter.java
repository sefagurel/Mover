package de.j4velin.systemappmover;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class z_EfficientAdapter extends BaseAdapter {

	private final LayoutInflater	mInflater;
	private final Handler			handler	= new Handler();
	private final AppPicker			ap;

	public z_EfficientAdapter(final Context c, final AppPicker a) {
		mInflater = LayoutInflater.from(c);
		ap = a;
	}

	public int getCount() {
		return ap.apps.size();
	}

	public Object getItem(int position) {
		return position;
	}

	public long getItemId(int position) {
		return position;
	}

	public View getView(final int position, View convertView, ViewGroup parent) {
		if (position < ap.apps.size()) {
			final ViewHolder holder;

			if (convertView == null || !(convertView.getTag() instanceof ViewHolder)) {
				convertView = mInflater.inflate(R.layout.listviewitem, null);
				holder = new ViewHolder();
				holder.text = (TextView) convertView.findViewById(R.id.text);
				holder.pack = (TextView) convertView.findViewById(R.id.pack);
				holder.icon = (ImageView) convertView.findViewById(R.id.icon);
				holder.system = (TextView) convertView.findViewById(R.id.system);
				convertView.setTag(holder);
			}
			else {
				holder = (ViewHolder) convertView.getTag();
			}

			handler.post(new Runnable() {
				public void run() {
					holder.text.setText(ap.apps.get(position).loadLabel(ap.pm));
					holder.pack.setText(ap.apps.get(position).packageName);
					holder.system.setVisibility(((ap.apps.get(position).flags & ApplicationInfo.FLAG_SYSTEM) == 1) ? View.VISIBLE : View.GONE);
					// sollte eig immer der fall sein, trotzdem
					// ArrayIndexOutofBounds crashreport erhalten
					if (position < ap.icons.size())
						holder.icon.setImageDrawable(ap.icons.get(position));
				}
			});

		}
		return convertView;
	}

	private class ViewHolder {
		TextView	text;
		TextView	pack;
		ImageView	icon;
		TextView	system;
	}

}