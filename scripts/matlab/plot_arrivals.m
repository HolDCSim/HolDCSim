%this script plolts the discrete data series for job arrivals
%read the matrix from file
figure;
arrivalsM = dlmread('job_arrivals_stats');
dim = size(M);
rows = dim(1);
stem(arrivalsM(1:end, 1), ones(rows,1), '-', 'Marker','none','Color',[0.5,0.5,0.5],'LineWidth',0.1);
%ylim([0,2]);
set(gca(), 'YTick', [0,1,2]);
set(gca(), 'Color','k');
set(gcf(), 'Position', [300,50,800,200]);
title(gca(),'mmpp job arrivals');
xlabel(gca, 'time');
%xlim([1,10]);

%plots mmpp status transitions
figure;
statesM = dlmread('mmpp_his');
stairs(statesM(1:end, 1), statesM(1:end,2), '-', 'Marker','none','Color',[0.5,0.5,0.5],'LineWidth',0.5);
set(gca(), 'YTick', [0,1,3]);
set(gca(), 'Color','k');
set(gcf(), 'Position', [300,50,800,200]);
ylim(gca, [0,2]);
title(gca(),'mmpp transitions');
xlabel(gca, 'time');

