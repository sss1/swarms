load('out.mat');

nTimes = size(positionsX, 1);

disp(['Displaying ' num2str(nTimes) ' frames...']);

% Fix axes
xMin = min(positionsX(:));
xMax = max(positionsX(:));
yMin = min(positionsY(:));
yMax = max(positionsY(:));

lag = 10;

figure('units','normalized','outerposition',[0 0 1 1]);
makeMovie = true;
for t = 1:200 %nTimes

  clf; hold all;
  scatter(positionsX(t, :), positionsY(t, :), 100*radii);
  ts = max(1, (t - lag)):t;
  if t > 1
    plot(positionsX(ts, :), positionsY(ts, :));
  end
  % axis([xMin xMax yMin yMax]);

  for wall_idx = 1:size(walls, 1)
    plot(walls(wall_idx, [1 3]), walls(wall_idx, [2 4]), 'b', 'LineWidth', 2);
  end

  axis([-10 60 -10 60]);
  if makeMovie
    M(t) = getframe;
  else
    pause(0.01); % Without pause, the display just buffers and doesn't have time to display live
  end
end
if makeMovie
  movie2avi(M, '/afs/andrew.cmu.edu/usr5/sss1/Desktop/swarms/videos/sim.avi');
  movie(M);
end
