load('out.mat');

nTimes = size(positionsX, 1);

% Fix axes
xMin = min(positionsX(:));
xMax = max(positionsX(:));
yMin = min(positionsY(:));
yMax = max(positionsY(:));

lag = 10;

figure;
for t = 1:nTimes

  clf; hold all;
  scatter(positionsX(t, :), positionsY(t, :), 10*radii);
  ts = max(1, (t - lag)):t;
  plot(positionsX(ts, :), positionsY(ts, :));
  axis([xMin xMax yMin yMax]);
  pause(0.01);

  for wall_idx = 1:size(walls, 1)
    plot(walls(wall_idx, [1 3]), walls(wall_idx, [2 4]));
  end

end
